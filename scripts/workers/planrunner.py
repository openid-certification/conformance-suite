import asyncio
import json
import logging
import string
import threading
import random
import time
import traceback
import os
import subprocess
import re
from colors import *

list1 = ["test1", "completed"]
result = ["SUCCESS", "WARNING", "FAILED", "TIMED_OUT"]

logger = logging.getLogger(__name__)

from helpers import split_name_and_variant, get_string_name_for_module_with_variant

ignored_modules = [
    # see https://gitlab.com/openid/conformance-suite/-/issues/837
    "oidcc-client-test-signing-key-rotation-just-before-signing",
    "oidcc-client-test-signing-key-rotation",
    # we have never created a test client that does the dcr process; however there is a test for it in the OP-against-RP tests
    "fapi1-advanced-final-client-brazildcr-happypath-test"
]


class PlanRunner(threading.Thread):
    def __init__(self,
                 task_queue,
                 status_queue,
                 conformance_server,
                 api_url_base,
                 output_dir,
                 config_parser,
                 result_queue):
        threading.Thread.__init__(self)
        self.daemon = True
        self._task_queue = task_queue
        self._status_queue = status_queue
        self._conformance_server = conformance_server
        self._api_url_base = api_url_base
        self._output_dir = output_dir
        self._config_parser = config_parser
        self._result_queue = result_queue
        self.start()

    def run(self):
        while task := self._task_queue.get_a_task():
            try:
                logger.debug(f"queue {task.queue_name} - got a task")
                self._status_queue.plan_starting(task.queue_name, task.config)
                test_plan_name = task.config["test"]["test_name"]
                variant = task.config["test"]["variants"]
                selected_modules = task.config["test"]["modules"]
                op_plan = task.config["op_test"]
                op_config = task.config["op_test"]["config_file"]

                brazil_client_scope = ''
                if variant != None and 'brazil_client_scope' in variant.keys():
                    brazil_client_scope = variant['brazil_client_scope']
                    del variant['brazil_client_scope']

                test_plan_info = self._conformance_server.create_test_plan(test_plan_name, task.config_json, variant)
                plan_id = test_plan_info['id']

                if selected_modules == None:
                    plan_modules = [module for module in test_plan_info['modules']
                                    if module['testModule'] not in ignored_modules]
                else:
                    plan_modules = [module for module in test_plan_info['modules']
                                    if get_string_name_for_module_with_variant(module) in selected_modules]

                test_info = {}  # key is module name
                test_info["variant"] = variant
                test_time_taken = {}  # key is testname
                overall_start_time = time.time()
                for moduledict in plan_modules:
                    logger.info(f"queue {task.queue_name} - starting test: {test_plan_name} ({plan_id}) - {moduledict['testModule']}")
                    test_start_time = time.time()
                    self.run_test_module(task, test_plan_name, moduledict, plan_id, test_info,
                                         variant, op_plan, op_config, brazil_client_scope, task.parsed_config)
                    time_taken = time.time() - test_start_time
                    test_time_taken[moduledict['testModule']] = time_taken
                    logger.info(f"queue {task.queue_name} - finished test: {test_plan_name} ({plan_id}) - {moduledict['testModule']} - {time_taken} seconds")
                logger.info(f"queue {task.queue_name} - finished test: {test_plan_name} ({plan_id}) - {time.time() - overall_start_time} seconds")
                self._result_queue.add_task_result(task, test_info)
                logger.debug(f"queue {task.queue_name} - plan {plan_id}  marking finished")
                self._status_queue.plan_finished(task.queue_name, plan_id)
                logger.debug(f"queue {task.queue_name} - plan {plan_id}  marked finished")
            except:
                print(traceback.format_exc())
            finally:
                logger.debug(f"queue {task.queue_name}  marking task completed")
                self._task_queue.task_completed(task)
                logger.debug(f"queue {task.queue_name} task completed")

    def run_test_module(self, task, test_plan_name,  moduledict, plan_id, test_info, variant, op_plan, op_config,
                         brazil_client_scope, parsed_config):
        module = moduledict['testModule']
        module_with_variants = get_string_name_for_module_with_variant(moduledict)

        module_id = ''
        module_info = {}

        try:
            test_module_info = self._conformance_server.create_test_from_plan_with_variant(plan_id, module,
                                                                                    moduledict.get('variant'))
            module_id = test_module_info['id']
            module_info['id'] = module_id
            test_info[get_string_name_for_module_with_variant(moduledict)] = module_info

            state = self._conformance_server.wait_for_state(module_id, ["CONFIGURED", "WAITING", "FINISHED"])
            if state == "CONFIGURED":
                if module == 'oidcc-server-rotate-keys':
                    # This test needs manually started once the OP keys have been rotated; we can't actually do that
                    # but at least we can run the test and check it finishes even if it always fails.
                    print('Starting test')
                    self._conformance_server.start_test(module_id)
                state = self._conformance_server.wait_for_state(module_id, ["WAITING", "FINISHED"])

            if state == "WAITING":
                # If it's a client test, we need to run the client.
                if op_plan is not None and len(op_plan) > 0:
                    if op_plan["test_name"] == "sample-openid-client-nodejs":
                        client_metadata_defaults = op_plan["variants"] if "variants" in op_plan else {}
                        client_metadata_defaults_str = json.dumps(client_metadata_defaults)
                        alias = parsed_config["alias"]
                        os.environ['ISSUER'] = os.environ["CONFORMANCE_SERVER"] + "test/a/" + alias + "/"
                        os.putenv('CLIENT_METADATA_DEFAULTS', client_metadata_defaults_str)

                        other_environment_vars_for_script = op_plan["environment"] if "environment" in op_plan else {}
                        for envvarname, val in other_environment_vars_for_script.items():
                            os.putenv(envvarname, val)
                        # Pass module variant into VARIANT in environment for distinguishing oidcc-client tests which have the same module id
                        variantstr = json.dumps(variant)
                        os.putenv('VARIANT', variantstr)
                        os.putenv('MODULE_NAME', module)
                        os.putenv('NODE_TLS_REJECT_UNAUTHORIZED','0')

                        subprocess.call(["npm", "run", "client"], cwd="./sample-openid-client-nodejs")
                        self._conformance_server.wait_for_state(module_id, ["FINISHED"])
                    else:
                        # the 'client' is our own OP tests
                        op_tests = {}
                        module_info['op'] = op_tests
                        op_modules = {}
                        op_tests['tests'] = op_modules
                        op_test_plan_name = op_plan["test_name"]
                        op_variant = op_plan["variants"] if "variants" in op_plan else {}
                        op_selected_modules = op_plan["modules"] if "modules" in op_plan else {}

                        op_tests['variant'] = op_variant
                        op_tests['config'] = op_config

                        logger.debug(f"queue {task.queue_name} - running test: {test_plan_name} - {moduledict['testModule']} - Creating OP test: {op_test_plan_name}")

                        (op_json_config, op_parsed_config) = self._config_parser.parse(op_config)
                        op_test_plan_info = self._conformance_server.create_test_plan(op_test_plan_name, op_json_config, op_variant)
                        op_plan_id = op_test_plan_info['id']
                        op_test_info = {}

                        op_plan_modules = [module for module in op_test_plan_info['modules'] if get_string_name_for_module_with_variant(module) in op_selected_modules]
                        for op_moduledict in op_plan_modules:
                            op_module = op_moduledict['testModule']
                            logger.debug(f"queue {task.queue_name} - running test: {test_plan_name} - {moduledict['testModule']} - running OP test: {op_test_plan_name} - {op_module}")
                            op_module_with_variants = get_string_name_for_module_with_variant(op_moduledict)
                            op_module_info = {}
                            op_modules[op_test_plan_name] = op_module_info
                            op_test_module_info = self._conformance_server.create_test_from_plan_with_variant(op_plan_id, op_module,
                                                               op_moduledict.get('variant'))
                            op_module_id = op_test_module_info['id']
                            op_module_info['id'] = op_module_id
                            op_test_info[get_string_name_for_module_with_variant(op_moduledict)] = op_module_info
                            try:
                                self._conformance_server.wait_for_state(op_module_id, ["FINISHED"])
                            except Exception as e:
                                traceback.print_exc()
                                print('Exception: Test {} {} failed to run to completion: {}'.format(op_module_with_variants, op_module_id, e))
                            op_module_info['info'] = self._conformance_server.get_module_info(op_module_id)
                            op_module_info['logs'] = self._conformance_server.get_test_log(op_module_id)


                elif re.match(r'fapi-rw-id2-client-.*', module) or \
                        re.match(r'fapi1-advanced-final-client-.*', module):
                    print("FAPI client test: " + module + " " + json.dumps(variant))
                    if brazil_client_scope:
                        os.environ['BRAZIL_CLIENT_SCOPE'] = brazil_client_scope
                    profile = variant['fapi_profile']
                    alias = parsed_config["alias"]
                    client_auth_type = variant['client_auth_type']
                    if profile == "openbanking_ksa":
                        if client_auth_type == "mtls":
                            subprocess.call(["./ksa-rp-client", "--alias", "ksa-rp",
                                             "--clientid", "bc680915-bbd3-45d7-b3c6-2716f4d178ed",
                                             "--transportCert", "./model_bank/transport.crt",
                                             "--transportKey", "./model_bank/transport.key",
                                             "--signingKey", "./model_bank/signing.key",
                                             "--encryptionKey", "./model_bank/encryption.key",
                                             "--serverBaseUrl", os.environ["CONFORMANCE_SERVER"],
                                             "--serverBaseMtlsUrl", os.environ["CONFORMANCE_SERVER_MTLS"]],
                                            cwd="./ksa-rp-client/")
                        else:
                            subprocess.call(["./ksa-rp-client", "--alias", "ksa-rp",
                                             "--clientid", "bc680915-bbd3-45d7-b3c6-2716f4d178ed",
                                             "--transportCert", "./model_bank/transport.crt",
                                             "--transportKey", "./model_bank/transport.key",
                                             "--signingKey", "./model_bank/signing.key",
                                             "--encryptionKey", "./model_bank/encryption.key",
                                             "--serverBaseUrl", os.environ["CONFORMANCE_SERVER"],
                                             "--serverBaseMtlsUrl", os.environ["CONFORMANCE_SERVER_MTLS"],
                                             "--privateKeyAuth"], cwd="./ksa-rp-client/")
                    else:
                        os.environ['ISSUER'] = os.environ["CONFORMANCE_SERVER"] + "test/a/" + alias + "/"
                        os.environ['ACCOUNTS'] = 'test-mtls/a/' + alias + '/open-banking/v1.1/accounts'
                        os.environ['ACCOUNT_REQUEST'] = 'test/a/' + alias + '/open-banking/v1.1/account-requests'
                        os.environ[
                            'BRAZIL_CONSENT_REQUEST'] = 'test-mtls/a/' + alias + '/open-banking/consents/v3/consents'
                        os.environ[
                            'BRAZIL_PAYMENTS_CONSENT_REQUEST'] = 'test-mtls/a/' + alias + '/open-banking/payments/v4/consents'
                        os.environ[
                            'BRAZIL_ACCOUNTS_ENDPOINT'] = 'test-mtls/a/' + alias + '/open-banking/accounts/v2/accounts'
                        os.environ[
                            'BRAZIL_PAYMENT_INIT_ENDPOINT'] = 'test-mtls/a/' + alias + '/open-banking/payments/v4/pix/payments'

                        os.environ['FAPI_PROFILE'] = profile
                        if 'fapi_auth_request_method' in variant.keys() and variant['fapi_auth_request_method']:
                            os.environ['FAPI_AUTH_REQUEST_METHOD'] = variant['fapi_auth_request_method']
                        else:
                            os.environ['FAPI_AUTH_REQUEST_METHOD'] = 'by_value'
                        if 'fapi_response_mode' in variant.keys() and variant['fapi_response_mode']:
                            os.environ['FAPI_RESPONSE_MODE'] = variant['fapi_response_mode']
                        else:
                            os.environ['FAPI_RESPONSE_MODE'] = 'plain_response'
                        if 'fapi_client_type' in variant.keys() and variant['fapi_client_type']:
                            os.environ['FAPI_CLIENT_TYPE'] = variant['fapi_client_type']
                        else:
                            os.environ['FAPI_CLIENT_TYPE'] = 'oidc'

                        os.environ['TEST_MODULE_NAME'] = module
                        subprocess.call(["npm", "run", "client"], cwd="./sample-openbanking-client-nodejs")
                self._conformance_server.wait_for_state(module_id, ["FINISHED"])

        except Exception as e:
            traceback.print_exc()
            print('Exception: Test {} {} failed to run to completion: {}'.format(module_with_variants, module_id, e))
        if module_id != '':
            module_info['info'] = self._conformance_server.get_module_info(module_id)
            module_info['logs'] = self._conformance_server.get_test_log(module_id)
