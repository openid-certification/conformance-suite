#!/usr/bin/env python3

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import argparse
import asyncio
import datetime
import fnmatch
import json
import os
import re
import subprocess
import sys
import time
import traceback

from conformance import Conformance

# Modules list here are deliberately not run, as they have known problems
ignored_modules = [
    # see https://gitlab.com/openid/conformance-suite/-/issues/837
    "oidcc-client-test-signing-key-rotation-just-before-signing",
    "oidcc-client-test-signing-key-rotation"
]

# Wrapper that adds timestamps to the start of our output
#
# This is mainly useful when the test is running inside gitlab, so we can see what exact time each step happened at
class timestamp_filter:
    # pending_output stores any output passed to write where a \n has not yet been found
    pending_output = ''
    def write(self, message):
        output = self.pending_output + message
        (output, not_used, self.pending_output) =  output.rpartition('\n')
        if output != '':
            timestamp = time.strftime("%Y-%m-%d %X")
            output = timestamp + " " + output.replace("\n", "\n"+timestamp+" ")
            print(output, file=sys.__stdout__)
            sys.__stdout__.flush()
    def flush(self):
        pass

sys.stdout = timestamp_filter()

# syntax is:
# test_plan_name[variant=value][variant2=value2]:optional-run-only-module-named-test{optestplan}optestconfig
# optestplan/config are only used when running the rp tests against the op tests
def split_name_and_variant(test_plan):
    modules = None
    op_test = None
    op_config = None
    if '{' in test_plan:
        (test_plan, op_test) = test_plan.split("{", 1)
        (op_test, op_config) = op_test.split("}", 1)
    if ':' in test_plan:
        (test_plan, module) = test_plan.split(":", 1)
        modules = module.split(",")
    if '[' in test_plan:
        name = re.match(r'^[^\[]*', test_plan).group(0)
        vs = re.finditer(r'\[([^=\]]*)=([^\]]*)\]', test_plan)
        variant = { v.group(1) : v.group(2) for v in vs }
        return (name, variant, modules, op_test, op_config)
    elif '(' in test_plan:
        #only for oidcc RP tests
        matches = re.match(r'(.*)\((.*)\)$', test_plan)
        name = matches.group(1)
        oidcc_configfile = matches.group(2)
        return (name, oidcc_configfile, modules, op_test, op_config)
    else:
        return (test_plan, None, modules, op_test, op_config)

#Run OIDCC RP tests
#OIDCC RP tests use a configuration file instead of providing all options in run-tests.sh
#this function runs plans in a loop and returns an array of results
async def run_test_plan_oidcc_rp(test_plan_name, config_file, json_config, oidcc_rptest_configfile, output_dir):
    oidcc_test_config_json = []
    with open(oidcc_rptest_configfile) as f:
        oidcc_test_config = f.read()
        oidcc_test_config_json = json.loads(oidcc_test_config)
    all_plan_results = []
    overall_starttime = time.time()
    parsed_config = json.loads(json_config)

    for test_plan_config in oidcc_test_config_json['tests']:
        client_metadata_defaults = test_plan_config['client_metadata_defaults']
        client_metadata_defaults_str = json.dumps(client_metadata_defaults)
        other_environment_vars_for_script = {}
        try:
            if test_plan_config['other_environment_variables']:
                other_environment_vars_for_script = test_plan_config['other_environment_variables']
                del test_plan_config['other_environment_variables']
        except:
            pass
        #remove client_metadata_defaults otherwise plan api call will fail
        del test_plan_config['client_metadata_defaults']
        test_plan_info = await conformance.create_test_plan(test_plan_name, json_config, test_plan_config)

        variantstr = json.dumps(test_plan_config)
        print('VARIANT {}'.format(variantstr))

        plan_id = test_plan_info['id']
        plan_modules = test_plan_info['modules']
        test_info = {}  # key is module name
        test_time_taken = {}  # key is module_id
        start_time_for_plan = time.time()
        print('Created test plan, new id: {}'.format(plan_id))
        print('{}plan-detail.html?plan={}'.format(api_url_base, plan_id))
        print('{:d} modules to test:\n{}\n'.format(len(plan_modules), '\n'.join(mod['testModule'] for mod in plan_modules)))
        for moduledict in plan_modules:
            module=moduledict['testModule']
            module_with_variants = get_string_name_for_module_with_variant(moduledict)
            if module in ignored_modules:
                continue
            test_start_time = time.time()
            module_id = ''
            log_detail_url = ''
            module_info = {}

            try:
                print('Running test module: {}'.format(module_with_variants))
                test_module_info = await conformance.create_test_from_plan_with_variant(plan_id, module, moduledict.get('variant'))
                module_id = test_module_info['id']
                module_info['id'] = module_id
                test_info[get_string_name_for_module_with_variant(moduledict)] = module_info
                print('Created test module, new id: {}'.format(module_id))
                log_detail_url = '{}log-detail.html?log={}'.format(api_url_base, module_id)
                print(log_detail_url)

                state = await conformance.wait_for_state(module_id, ["WAITING", "FINISHED"])

                if state == "WAITING":
                    alias = parsed_config["alias"]
                    oidcc_issuer_str = os.environ["CONFORMANCE_SERVER"] + "test/a/" + alias + "/"
                    print('ISSUER {}'.format(oidcc_issuer_str))
                    print('CLIENT_METADATA_DEFAULTS {}'.format(client_metadata_defaults_str))

                    os.putenv('CLIENT_METADATA_DEFAULTS', client_metadata_defaults_str)

                    if other_environment_vars_for_script:
                        for envvarname in other_environment_vars_for_script:
                            os.putenv(envvarname, other_environment_vars_for_script[envvarname])
                    # Pass module variant into VARIANT in environment for distinguishing oidcc-client tests which have the same module id
                    if moduledict.get('variant') != None:
                        variantstr = json.dumps({**test_plan_config, **moduledict.get('variant')})
                    os.putenv('VARIANT', variantstr)
                    os.putenv('MODULE_NAME', module)
                    os.putenv('ISSUER', oidcc_issuer_str)
                    subprocess.call(["npm", "run", "client"], cwd="./sample-openid-client-nodejs")

                    await conformance.wait_for_state(module_id, ["FINISHED"])

            except Exception as e:
                traceback.print_exc()
                print('Exception: Test {} {} failed to run to completion: {} {}'.format(module_with_variants, module_id, e, log_detail_url))
            if module_id != '':
                test_time_taken[module_id] = time.time() - test_start_time
                module_info['info'] = await conformance.get_module_info(module_id)
                module_info['logs'] = await conformance.get_test_log(module_id)

        time_for_plan = time.time() - start_time_for_plan
        print('Finished test plan - id: {} total time: {}'.format(plan_id, time_for_plan))
        if output_dir != None:
            start_time_for_save = time.time()
            filename = await conformance.exporthtml(plan_id, output_dir)
            print('{} results saved to "{}" in {:.1f} seconds'.format(plan_id, filename, time.time() - start_time_for_save))
        print('\n\n')
        result_for_plan = {
            'test_plan': test_plan_name,
            'config_file': config_file,
            'plan_id': plan_id,
            'plan_modules': plan_modules,
            'test_info': test_info,
            'test_time_taken': test_time_taken,
            'overall_time': time_for_plan
        }
        all_plan_results.append(result_for_plan)

    overall_time = time.time() - overall_starttime
    print('--- Finished OIDCC RP tests. Total time: {} '.format(overall_time))
    return all_plan_results

# If testmodule has variants, return a string form like used on our command line
# e.g.
# oidcc-server[client_auth_type=client_secret_basic][response_mode=default][response_type=code]
# This is useful for using as a dictionary key for storing results
def get_string_name_for_module_with_variant(moduledict):
    name = moduledict['testModule']
    variants = moduledict.get('variant')
    if variants != None:
        for v in sorted(variants.keys()):
            name += "[{}={}]".format(v, variants[v])
    return name

async def queue_worker(q):
    while True:
        code = await q.get()
        try:
            await code
        except Exception as e:
            # log and ignore all exceptions, as run_queue otherwise locks up
            print('Exception caught in queue_worker:')
            traceback.print_exc()
            sys.exit(1)
        finally:
            q.task_done()

async def run_queue(q, parallel_jobs):
    workers = [asyncio.create_task(queue_worker(q)) for _ in range(parallel_jobs)]
    await q.join()  # wait for all tasks to be processed
    print("queue done, cancelling workers")
    for worker in workers:
        worker.cancel()
    print("workers cancelled, gathering")
    await asyncio.gather(*workers, return_exceptions=True)
    print("workers gathered")

async def run_test_plan(test_plan, config_file, output_dir):
    print("Running plan '{}' with configuration file '{}'".format(test_plan, config_file))
    start_section(test_plan, "Results", True)
    with open(config_file) as f:
        json_config = f.read()
    json_config = json_config.replace('{BASEURL}', os.environ['CONFORMANCE_SERVER'])
    (test_plan_name, variant, selected_modules, op_plan, op_config) = split_name_and_variant(test_plan)
    if test_plan_name.startswith('oidcc-client-'):
        #for oidcc client tests 'variant' will contain the rp tests configuration file name
        return await run_test_plan_oidcc_rp(test_plan_name, config_file, json_config, variant, output_dir)
    brazil_client_scope = ''
    if variant != None and 'brazil_client_scope' in variant.keys():
        brazil_client_scope = variant['brazil_client_scope']
        del variant['brazil_client_scope']
    test_plan_info = await conformance.create_test_plan(test_plan_name, json_config, variant)
    plan_id = test_plan_info['id']
    parsed_config = json.loads(json_config)
    parallel_jobs = 3
    if args.no_parallel:
        parallel_jobs = 1
        print("{}: no-parallel command line argument passed - not running tests within this plan in parallel".format(plan_id))
    elif args.no_parallel_for_no_alias:
        parallel_jobs = 1
        print("{}: no-parallel-for-no-alias command line argument passed - not running tests within this plan in parallel".format(plan_id))
    elif "alias" in parsed_config:
        parallel_jobs = 1
        print("{}: Config '{}' contains alias '{}' - not running tests in parallel. If the test supports dynamic client registration and you have enabled it, you can remove the alias from your configuration file to speed up tests.".format(plan_id, config_file, parsed_config["alias"]))
    if selected_modules == None:
        plan_modules = test_plan_info['modules']
    else:
        plan_modules = [module for module in test_plan_info['modules'] if get_string_name_for_module_with_variant(module) in selected_modules]
    if len(plan_modules) == 0:
        raise Exception("No modules to test in " + test_plan_name)

    test_info = {}  # key is module name
    test_time_taken = {}  # key is module_id
    overall_start_time = time.time()
    plan_results = []
    print('Created test plan, new id: {}'.format(plan_id))
    print('{}plan-detail.html?plan={}'.format(api_url_base, plan_id))
    print('{:d} modules to test:\n{}\n'.format(len(plan_modules), '\n'.join(mod['testModule'] for mod in plan_modules)))
    queue = asyncio.Queue()
    for moduledict in plan_modules:
        queue.put_nowait(run_test_module(moduledict, plan_id, test_info, test_time_taken, variant, op_plan, op_config, plan_results, output_dir, brazil_client_scope, parsed_config))
    await run_queue(queue, parallel_jobs)

    overall_time = time.time() - overall_start_time
    if output_dir != None:
        start_time_for_save = time.time()
        filename = await conformance.exporthtml(plan_id, output_dir)
        print('results saved to "{}" in {:.1f} seconds'.format(filename, time.time() - start_time_for_save))
    end_section(test_plan)
    print('\n\n')
    plan_results.append({
        'test_plan': test_plan,
        'config_file': config_file,
        'plan_id': plan_id,
        'plan_modules': plan_modules,
        'test_info': test_info,
        'test_time_taken': test_time_taken,
        'overall_time': overall_time
    })
    return plan_results


async def run_test_module(moduledict, plan_id, test_info, test_time_taken, variant, op_plan, op_config, plan_results, output_dir, brazil_client_scope, parsed_config):
    module=moduledict['testModule']
    module_with_variants = get_string_name_for_module_with_variant(moduledict)
    test_start_time = time.time()
    module_id = ''
    module_info = {}

    try:
        print('Running test module: {}'.format(module_with_variants))
        test_module_info = await conformance.create_test_from_plan_with_variant(plan_id, module, moduledict.get('variant'))
        module_id = test_module_info['id']
        module_info['id'] = module_id
        test_info[get_string_name_for_module_with_variant(moduledict)] = module_info
        print('Created test module, new id: {}'.format(module_id))
        print('{}log-detail.html?log={}'.format(api_url_base, module_id))

        state = await conformance.wait_for_state(module_id, ["CONFIGURED", "WAITING", "FINISHED"])
        if state == "CONFIGURED":
            if module == 'oidcc-server-rotate-keys':
                # This test needs manually started once the OP keys have been rotated; we can't actually do that
                # but at least we can run the test and check it finishes even if it always fails.
                print('Starting test')
                await conformance.start_test(module_id)
            state = await conformance.wait_for_state(module_id, ["WAITING", "FINISHED"])

        if state == "WAITING":
            # If it's a client test, we need to run the client.
            # please note oidcc client tests are handled in a separate method. only FAPI ones will reach here
            if op_plan != None:
                # the 'client' is our own OP tests
                plan_results.extend(await run_test_plan(op_plan, op_config, output_dir))
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
                                         "--serverBaseUrl", os.environ["CONFORMANCE_SERVER"] ], cwd="./ksa-rp-client/")
                    else:
                        subprocess.call(["./ksa-rp-client", "--alias", "ksa-rp",
                                         "--clientid", "bc680915-bbd3-45d7-b3c6-2716f4d178ed",
                                         "--transportCert", "./model_bank/transport.crt",
                                         "--transportKey", "./model_bank/transport.key",
                                         "--signingKey", "./model_bank/signing.key",
                                         "--encryptionKey", "./model_bank/encryption.key",
                                         "--serverBaseUrl", os.environ["CONFORMANCE_SERVER"],
                                         "--privateKeyAuth"], cwd="./ksa-rp-client/")
                else:
                    os.environ['ISSUER'] = os.environ["CONFORMANCE_SERVER"] + "test/a/" + alias + "/"
                    os.environ['ACCOUNTS'] = 'test-mtls/a/' + alias + '/open-banking/v1.1/accounts'
                    os.environ['ACCOUNT_REQUEST'] = 'test/a/' + alias + '/open-banking/v1.1/account-requests'
                    os.environ['BRAZIL_CONSENT_REQUEST'] = 'test-mtls/a/' + alias + '/consents/v1/consents'
                    os.environ['BRAZIL_PAYMENTS_CONSENT_REQUEST'] = 'test-mtls/a/' + alias + '/payments/v1/consents'
                    os.environ['BRAZIL_ACCOUNTS_ENDPOINT'] = 'test-mtls/a/' + alias + '/accounts/v1/accounts'
                    os.environ['BRAZIL_PAYMENT_INIT_ENDPOINT'] = 'test-mtls/a/' + alias + '/payments/v1/pix/payments'

                    os.environ['FAPI_PROFILE'] = profile
                    if 'fapi_auth_request_method' in variant.keys() and variant['fapi_auth_request_method']:
                        os.environ['FAPI_AUTH_REQUEST_METHOD'] =  variant['fapi_auth_request_method']
                    else:
                        os.environ['FAPI_AUTH_REQUEST_METHOD'] = 'by_value'
                    if 'fapi_response_mode' in variant.keys() and variant['fapi_response_mode']:
                        os.environ['FAPI_RESPONSE_MODE'] =  variant['fapi_response_mode']
                    else:
                        os.environ['FAPI_RESPONSE_MODE'] = 'plain_response'
                    if 'fapi_client_type' in variant.keys() and variant['fapi_client_type']:
                        os.environ['FAPI_CLIENT_TYPE'] =  variant['fapi_client_type']
                    else:
                        os.environ['FAPI_CLIENT_TYPE'] = 'oidc'

                    os.environ['TEST_MODULE_NAME'] = module
                    subprocess.call(["npm", "run", "client"], cwd="./sample-openbanking-client-nodejs")

            await conformance.wait_for_state(module_id, ["FINISHED"])

    except Exception as e:
        traceback.print_exc()
        print('Exception: Test {} {} failed to run to completion: {}'.format(module_with_variants, module_id, e))
    if module_id != '':
        test_time_taken[module_id] = time.time() - test_start_time
        module_info['info'] = await conformance.get_module_info(module_id)
        module_info['logs'] = await conformance.get_test_log(module_id)


# from http://stackoverflow.com/a/26445590/3191896 and https://gist.github.com/Jossef/0ee20314577925b4027f
def color(text, **user_styles):

    styles = {
        # styles
        'reset': '\033[0m',
        'bold': '\033[01m',
        'disabled': '\033[02m',
        'underline': '\033[04m',
        'reverse': '\033[07m',
        'strike_through': '\033[09m',
        'invisible': '\033[08m',
        # text colors
        'fg_black': '\033[30m',
        'fg_red': '\033[31m',
        'fg_green': '\033[32m',
        'fg_orange': '\033[33m',
        'fg_blue': '\033[34m',
        'fg_purple': '\033[35m',
        'fg_cyan': '\033[36m',
        'fg_light_grey': '\033[37m',
        'fg_dark_grey': '\033[90m',
        'fg_light_red': '\033[91m',
        'fg_light_green': '\033[92m',
        'fg_yellow': '\033[93m',
        'fg_light_blue': '\033[94m',
        'fg_pink': '\033[95m',
        'fg_light_cyan': '\033[96m',
        # background colors
        'bg_black': '\033[40m',
        'bg_red': '\033[41m',
        'bg_green': '\033[42m',
        'bg_orange': '\033[43m',
        'bg_blue': '\033[44m',
        'bg_purple': '\033[45m',
        'bg_cyan': '\033[46m',
        'bg_light_grey': '\033[47m'
    }

    color_text = ''
    for style in user_styles:
        try:
            color_text += styles[style]
        except KeyError:
            return 'def color: parameter {} does not exist'.format(style)
    color_text += text
    return '\033[0m{}\033[0m'.format(color_text)


def redbg(text):
    return color(text, bold=True, bg_red=True)

def failure(text):
    return color(text, bold=True, fg_red=True)


def warning(text):
    return color(text, bold=True, fg_orange=True)


def success(text):
    return color(text, fg_green=True)


def expected_warning(text):
    return color(text, fg_pink=True)


def expected_failure(text):
    return color(text, fg_light_cyan=True)


def show_plan_results(plan_result, analyzed_result):
    plan_id = plan_result['plan_id']
    plan_modules = plan_result['plan_modules']
    test_info = plan_result['test_info']
    test_time_taken = plan_result['test_time_taken']
    overall_time = plan_result['overall_time']
    detail_plan_result = analyzed_result['detail_plan_result']

    incomplete = 0
    successful_conditions = 0
    overall_test_results = list(detail_plan_result['overall_test_results'])

    number_of_failures = 0
    number_of_warnings = 0

    counts_unexpected = detail_plan_result['counts_unexpected']

    for moduledict in plan_modules:
        module_name = get_string_name_for_module_with_variant(moduledict)
        if module_name in ignored_modules:
            continue
        if module_name not in test_info:
            print(failure('Test {} did not run'.format(module_name)))
            continue
        module_info = test_info[module_name]
        module_id = module_info['id']
        info = module_info['info']
        logs = module_info['logs']

        status_coloured = info['status']

        if info['status'] != 'FINISHED' and info['status'] != 'INTERRUPTED':
            status_coloured = redbg(status_coloured)
            incomplete += 1

        test_name = info['testName']
        result = overall_test_results.pop(0)['test_result']

        if module_id in test_time_taken:
            test_time = test_time_taken[module_id]
        else:
            test_time = -1
        if info['result'] == 'PASSED':
            result_coloured = success(info['result'])
        elif info['result'] == 'WARNING':
            result_coloured = warning(info['result'])
        elif info['result'] == 'REVIEW':
            result_coloured = color(info['result'], bold=True, fg_light_blue=True)
        else:
            result_coloured = failure(info['result'])

        counts = result['counts']
        print('Test {} {} {} - result {}. {:d} log entries - {:d} SUCCESS {:d} FAILURE, {:d} WARNING, {:.1f} seconds'.
              format(module_name, module_id, status_coloured, result_coloured, len(logs),
                     counts['SUCCESS'], counts['FAILURE'], counts['WARNING'], test_time))

        summary_unexpected_failures_test_module(result, test_name, module_id)

        successful_conditions += counts['SUCCESS']
        number_of_failures += counts['FAILURE']
        number_of_warnings += counts['WARNING']

    print(
        '\nOverall totals: ran {:d} test modules. '
        'Conditions: {:d} successes, {:d} failures, {:d} warnings. {:.1f} seconds'.
        format(len(test_info), successful_conditions, number_of_failures, number_of_warnings, overall_time))
    print('\n{}plan-detail.html?plan={}\n'.format(api_url_base, plan_id))

    if len(test_info) != len(plan_modules):
        print(failure("** NOT ALL TESTS FROM PLAN WERE RUN **"))
        return

    if incomplete != 0:
        print(failure("** {:d} TEST MODULES DID NOT RUN TO COMPLETION **".format(incomplete)))
        return

    if counts_unexpected['UNEXPECTED_FAILURES'] > 0:
        print(failure("** SOME TEST MODULES HAVE CONDITION UNEXPECTED FAILURES **"))

    if  counts_unexpected['UNEXPECTED_WARNINGS'] > 0:
        print(failure("** SOME TEST MODULES HAVE CONDITION UNEXPECTED WARNINGS **"))

    if counts_unexpected['UNEXPECTED_SKIPS'] > 0:
        print(failure("** SOME TEST MODULES WERE UNEXPECTEDLY SKIPPED **"))

    if counts_unexpected['EXPECTED_FAILURES_NOT_HAPPEN'] > 0:
        print(failure("** SOME TEST MODULES HAVE CONDITION EXPECTED FAILURE DID NOT HAPPEN **"))

    if counts_unexpected['EXPECTED_WARNINGS_NOT_HAPPEN'] > 0:
        print(failure("** SOME TEST MODULES HAVE CONDITION EXPECTED WARNING DID NOT HAPPEN **"))

    if counts_unexpected['EXPECTED_SKIPS_NOT_HAPPEN'] > 0:
        print(failure("** SOME TEST MODULES WERE EXPECTED TO BE SKIPPED BUT COMPLETED **"))


# Returns object contains 'plan_did_not_complete' and 'detail_plan_result', ie.
# 'plan_did_not_complete' = NOT_COMPLETE
#     some test modules did not run complete
# 'plan_did_not_complete' = FAILURE_OR_WARNING
#     if any test unexpectedly failed to run to completion
#     if any test was expected to be skipped but wasn't
#     if condition failure/warning occurs and isn't listed in the config json file
#     if failure/warning is expected and doesn't occur
# 'plan_did_not_complete' = COMPLETE
#     all test modules run complete
def analyze_plan_results(plan_result, expected_failures_list, expected_skips_list):
    plan_modules = plan_result['plan_modules']
    test_info = plan_result['test_info']

    incomplete = 0
    overall_test_results = []
    have_ignored_modules = False

    counts_unexpected = {
        'EXPECTED_FAILURES': 0,
        'UNEXPECTED_FAILURES': 0,
        'EXPECTED_FAILURES_NOT_HAPPEN': 0,
        'EXPECTED_WARNINGS': 0,
        'UNEXPECTED_WARNINGS': 0,
        'EXPECTED_WARNINGS_NOT_HAPPEN': 0,
        'EXPECTED_SKIPS': 0,
        'UNEXPECTED_SKIPS': 0,
        'EXPECTED_SKIPS_NOT_HAPPEN': 0
    }

    for moduledict in plan_modules:
        module=moduledict['testModule']
        if module in ignored_modules:
            have_ignored_modules = True
            continue
        module_name_with_variant = get_string_name_for_module_with_variant(moduledict)
        if module_name_with_variant not in test_info:
            continue
        module_info = test_info[module_name_with_variant]
        module_id = module_info['id']
        info = module_info['info']
        logs = module_info['logs']

        if module in untested_test_modules:
            untested_test_modules.remove(module)

        if info['status'] != 'FINISHED' and info['status'] != 'INTERRUPTED':
            incomplete += 1
        if 'result' not in info or info['result'] is None:
            info['result'] = 'UNKNOWN'

        test_name = info['testName']
        result = analyze_result_logs(module_id, test_name, info['result'], plan_result, logs, expected_failures_list, expected_skips_list, counts_unexpected)

        log_detail_link = '{}log-detail.html?log={}'.format(api_url_base, module_id)
        test_result = {'test_name': test_name, 'log_detail_link': log_detail_link, 'test_result': result}
        overall_test_results.append(test_result)

    detail_plan_result = {
        'plan_name': plan_result['test_plan'],
        'plan_config_file': plan_result['config_file'],
        'overall_test_results': overall_test_results,
        'counts_unexpected': counts_unexpected
    }

    if (not have_ignored_modules and len(test_info) != len(plan_modules) or incomplete != 0):
        return {'plan_did_not_complete': 'NOT_COMPLETE', 'detail_plan_result': detail_plan_result}
    elif (counts_unexpected['UNEXPECTED_FAILURES']
          or counts_unexpected['UNEXPECTED_WARNINGS']
          or counts_unexpected['UNEXPECTED_SKIPS']
          or counts_unexpected['EXPECTED_FAILURES_NOT_HAPPEN']
          or counts_unexpected['EXPECTED_WARNINGS_NOT_HAPPEN']
          or counts_unexpected['EXPECTED_SKIPS_NOT_HAPPEN']):
        return {'plan_did_not_complete': 'FAILURE_OR_WARNING', 'detail_plan_result': detail_plan_result}
    else:
        return {'plan_did_not_complete': 'COMPLETE', 'detail_plan_result': detail_plan_result}


# Analyze all log of a test module and return object contains:
#   'expected_failures': list all expected failures condition
#   'unexpected_failures': list all unexpected failures condition
#   'expected_failures_did_not_happen': list all expected failures condition did not happen
#   'expected_warnings': list all expected warnings condition
#   'unexpected_warnings': list all unexpected warnings condition
#   'expected_warnings_did_not_happen': list all expected warnings condition did not happen
#   'counts': contains number of success condition, number of warning condition and number of failure condition
def analyze_result_logs(module_id, test_name, test_result, plan_result, logs, expected_failures_list, expected_skips_list, counts_unexpected):
    counts = {'SUCCESS': 0, 'WARNING': 0, 'FAILURE': 0}
    expected_failures = []
    unexpected_failures = []
    expected_failures_did_not_happen = []

    expected_warnings = []
    unexpected_warnings = []
    expected_warnings_did_not_happen = []

    expected_skip = False
    unexpected_skip = False
    expected_skip_did_not_happen = False

    test_plan = plan_result['test_plan']
    config_filename = plan_result['config_file']
    (test_plan_name, variant, _, _, _) = split_name_and_variant(test_plan)

    def is_expected_for_this_test(obj):
        """
        Check if an expected failure/warning read from an 'expected' JSON file matches the current test module

        The match is lose and allows the following:
        - configuration-filename may contain shell-style wildcards (mainly '*')
        - variant may be listed as a "*" to match all variants
        - if variant is an object, only the keys/values listed in JSON will be checked (i.e. the entry will be assumed
        to apply to all unlisted variants)

        :param obj: expected_failure_obj entry from json expected list to test
        :return: True if entry matches the current test module
        """
        if obj['test-name'] != test_name:
            return False
        if not fnmatch.fnmatch(config_filename, obj['configuration-filename']):
            return False
        expected_variant = obj.get('variant', None)
        if expected_variant == "*":
            return True
        for k in expected_variant:
            if not k in variant:
                return False
            if expected_variant[k] != variant[k]:
                return False
        return True

    test_expected_failures = list(filter(is_expected_for_this_test, expected_failures_list))
    test_expected_skips = list(filter(is_expected_for_this_test, expected_skips_list))

    block_names = {}
    block_msg = ''
    for log_entry in logs:
        if ('startBlock' in log_entry and log_entry['startBlock'] == True and log_entry['src'] == '-START-BLOCK-'):
            block_names[log_entry['blockId']] = log_entry['msg']
            continue
        if 'result' not in log_entry:
            continue

        if ('blockId' in log_entry):
            blockId = log_entry['blockId']
            if blockId in block_names:
                block_msg = block_names[blockId]
            else:
                # A new blockId was seen without a block start: this shouldn't happen.
                # We don't have a sensible value for block_msg at this point, so log the error and stop analyzing this test.
                print(failure('Unknown block ID in results: {}'.format(blockId)))
                print('See {}log-detail.html?log={}'.format(api_url_base, module_id))
                print('Log entry: {}'.format(log_entry))
                break
        else:
            block_msg = ''

        log_result = log_entry['result']  # contains WARNING/FAILURE/INFO/etc
        if log_result in counts:
            counts[log_result] += 1
            log_entry_exist_in_expected_list = False
            for expected_failure_obj in test_expected_failures:
                expected_condition = expected_failure_obj['condition']
                expected_block = expected_failure_obj['current-block']
                expected_result = expected_failure_obj['expected-result']

                if (expected_block == block_msg
                    and expected_condition == log_entry['src']):

                    # check and list all expected failure
                    if (log_result == 'FAILURE' and expected_result == 'failure'):
                        expected_failures.append({'current_block': block_msg, 'src': log_entry['src']})
                        counts_unexpected['EXPECTED_FAILURES'] += 1

                    # check and list all expected warning
                    elif (log_result == 'WARNING' and expected_result == 'warning'):
                        expected_warnings.append({'current_block': block_msg, 'src': log_entry['src']})
                        counts_unexpected['EXPECTED_WARNINGS'] += 1

                    # this wasn't an expected failure after all
                    else:
                        continue

                    log_entry_exist_in_expected_list = True
                    expected_failure_obj['__used'] = True
                    break

            # list all the unexpected failures/warnings of a test module
            if log_entry_exist_in_expected_list == False:
                if log_result == 'FAILURE':
                    unexpected_failures.append({'current_block': block_msg, 'src': log_entry['src']})
                    counts_unexpected['UNEXPECTED_FAILURES'] += 1

                if log_result == 'WARNING':
                    unexpected_warnings.append({'current_block': block_msg, 'src': log_entry['src']})
                    counts_unexpected['UNEXPECTED_WARNINGS'] += 1

    # list all the expected failures/warnings did not happen for a test module
    for expected_failure_obj in test_expected_failures:
        if '__used' in expected_failure_obj:
            continue
        expected_result = expected_failure_obj['expected-result']
        expected_block = expected_failure_obj['current-block']
        expected_condition = expected_failure_obj['condition']

        if expected_result == 'failure':
            expected_failures_did_not_happen.append({'current_block': expected_block, 'src': expected_condition})
            counts_unexpected['EXPECTED_FAILURES_NOT_HAPPEN'] += 1

        elif expected_result == 'warning':
            expected_warnings_did_not_happen.append({'current_block': expected_block, 'src': expected_condition})
            counts_unexpected['EXPECTED_WARNINGS_NOT_HAPPEN'] += 1

    for expected_skip_obj in test_expected_skips:
        if test_result == 'SKIPPED' or test_result == 'FAILED':
            expected_skip = True
            expected_skip_obj['__used'] = True
        else:
            expected_skip_did_not_happen = True
            counts_unexpected['EXPECTED_SKIPS_NOT_HAPPEN'] += 1

    if test_result == 'SKIPPED' and not expected_skip:
        unexpected_skip = True
        counts_unexpected['UNEXPECTED_SKIPS'] += 1

    # currently the ob intent id match fails for authlete and we don't list it as a 'skip', so we can't check this
    # this shouldn't matter much as all failed test results should have a 'FAILURE' log entry.
    #if test_result == 'FAILED' and not expected_skip:
    #    print(failure("Test result is FAILED: "+module_id))
    #    counts_unexpected['UNEXPECTED_FAILURES'] += 1

    if test_result != 'PASSED' and \
        test_result != 'WARNING' and \
        test_result != 'REVIEW' and \
        test_result != 'SKIPPED' and \
        test_result != 'FAILED':
        print(failure("Test result is an unexpected value, "+test_result+" for: "+module_id))
        counts_unexpected['UNEXPECTED_FAILURES'] += 1

    return {
        'expected_failures': expected_failures,
        'unexpected_failures': unexpected_failures,
        'expected_failures_did_not_happen': expected_failures_did_not_happen,
        'expected_warnings': expected_warnings,
        'unexpected_warnings': unexpected_warnings,
        'expected_warnings_did_not_happen': expected_warnings_did_not_happen,
        'expected_skip': expected_skip,
        'unexpected_skip': unexpected_skip,
        'expected_skip_did_not_happen': expected_skip_did_not_happen,
        'counts': counts
    }


# Output all unexpected failure for each test module
def summary_unexpected_failures_test_module(result, test_name, module_id):
    expected_failures = result['expected_failures']
    unexpected_failures = result['unexpected_failures']
    expected_failures_did_not_happen = result['expected_failures_did_not_happen']

    expected_warnings = result['expected_warnings']
    unexpected_warnings = result['unexpected_warnings']
    expected_warnings_did_not_happen = result['expected_warnings_did_not_happen']

    expected_skip = result['expected_skip']
    unexpected_skip = result['unexpected_skip']
    expected_skip_did_not_happen = result['expected_skip_did_not_happen']

    if len(expected_failures) > 0:
        print(expected_failure("Expected failure: "))
        print_failure_warning(expected_failures, 'failure', '\t', expected=True)

    if len(unexpected_failures) > 0:
        print(failure("Unexpected failure: "))
        print_failure_warning(unexpected_failures, 'failure', '\t')

    if len(expected_failures_did_not_happen) > 0:
        print(failure("Expected failure did not happen: "))
        print_failure_warning(expected_failures_did_not_happen, 'failure', '\t')

    if len(expected_warnings) > 0:
        print(expected_warning("Expected warning: "))
        print_failure_warning(expected_warnings, 'warning', '\t', expected=True)

    if len(unexpected_warnings) > 0:
        print(warning("Unexpected warning: "))
        print_failure_warning(unexpected_warnings, 'warning', '\t')

    if len(expected_warnings_did_not_happen) > 0:
        print(warning("Expected warning did not happen: "))
        print_failure_warning(expected_warnings_did_not_happen, 'warning', '\t')

    if expected_skip:
        print(expected_warning("Test was skipped as expected"))

    if unexpected_skip:
        print(warning("Test was unexpectedly skipped"))

    if expected_skip_did_not_happen:
        print(warning("Test completed but was expected to be skipped"))


# Output all unexpected failures for all test plan at the end of run-test-plan.py file
def summary_unexpected_failures_all_test_plan(detail_plan_results):
    for detail_plan_result in detail_plan_results:
        if detail_plan_result:
            config_filename = detail_plan_result['plan_config_file']
            test_plan = detail_plan_result['plan_name']
            print(failure('{} - {}: '.format(test_plan, config_filename)))
            overall_test_results = detail_plan_result['overall_test_results']
            counts_unexpected = detail_plan_result['counts_unexpected']
            (test_plan_name, variant, _, _, _) = split_name_and_variant(test_plan)

            if counts_unexpected['UNEXPECTED_FAILURES'] > 0:
                print(failure('\tUnexpected failure: '))
                output_summary_test_plan_by_unexpected_type(variant, config_filename, overall_test_results, 'unexpected_failures', 'failure')

            if counts_unexpected['EXPECTED_FAILURES_NOT_HAPPEN'] > 0:
                print(failure('\tExpected failure did not happen: '))
                output_summary_test_plan_by_unexpected_type(variant, config_filename, overall_test_results, 'expected_failures_did_not_happen', 'failure')

            if counts_unexpected['UNEXPECTED_WARNINGS'] > 0:
                print(warning('\tUnexpected warning: '))
                output_summary_test_plan_by_unexpected_type(variant, config_filename, overall_test_results, 'unexpected_warnings', 'warning')

            if counts_unexpected['EXPECTED_WARNINGS_NOT_HAPPEN'] > 0:
                print(warning('\tExpected warning did not happen: '))
                output_summary_test_plan_by_unexpected_type(variant, config_filename, overall_test_results, 'expected_warnings_did_not_happen', 'warning')
            if counts_unexpected['UNEXPECTED_SKIPS'] > 0:
                print(warning('\tUnexpected skip: '))
                output_summary_test_plan_by_unexpected_type(variant, config_filename, overall_test_results, 'unexpected_skip', 'warning')
            if counts_unexpected['EXPECTED_SKIPS_NOT_HAPPEN'] > 0:
                print(warning('\tExpected skip did not happen: '))
                output_summary_test_plan_by_unexpected_type(variant, config_filename, overall_test_results, 'expected_skip_did_not_happen', 'warning')


def output_summary_test_plan_by_unexpected_type(variant, config_filename, overall_test_results, key, unexpected_type):
    for test_result in overall_test_results:
        result = test_result['test_result']
        if result[key]:
            test_name = test_result['test_name']
            header = '\t\t{} {}'.format(test_name, test_result['log_detail_link'])
            if 'skip' in key:
                print(failure(header))
                continue
            if unexpected_type == 'failure':
                print(failure(header))
            else:
                print(warning(header))
            print_template = 'unexpected' in key
            print_failure_warning(result[key], unexpected_type, '\t\t\t', variant=variant, config=config_filename, test=test_name, print_template=print_template)


def print_failure_warning(failure_warning_list, status, tab_format, variant=None, expected=False, config=None, test=None, print_template=False):
    for failure_warning in failure_warning_list:
        msg = "{}Block name: '{}' - Condition: '{}'".format(tab_format,
                                                            failure_warning['current_block'],
                                                            failure_warning['src'])
        template = {
            'test-name': test,
            'variant': variant,
            'configuration-filename': config,
            'current-block': failure_warning['current_block'],
            'condition': failure_warning['src'],
            'expected-result': status,
            'comment': '**CHANGE ME** explain why this failure occurs'
        }
        if (status == 'failure'):
            if expected:
                print(expected_failure(msg))
            else:
                print(failure(msg))
                if print_template:
                    start_section("template_expected_failure", "Template expected failure json:", True)
                    # print json, skipping timestamp addition for easy C&P
                    print(json.dumps(template, indent=4)+",\n", file=sys.__stdout__)
                    end_section("template_expected_failure")
        else:
            if expected:
                print(expected_warning(msg))
            else:
                print(warning(msg))
                if print_template:
                    start_section("template_expected_warning", "Template expected warning json:", True)
                    # print json, skipping timestamp addition for easy C&P
                    print(json.dumps(template, indent=4)+",\n", file=sys.__stdout__)
                    end_section("template_expected_warning")


def load_expected_problems(filespec):
    # read json config file which records a list of expected failures/warnings
    all_loaded = []
    for fname in filespec.split("|"):
        with open(fname) as f:
            data = f.read();
            if data:
                all_loaded.extend(json.loads(data))

    # Check for duplicates
    seen = []
    duplicates = []
    filtered = []
    for item in all_loaded:
        key = tuple(item.get(field, None) for field in ['test-name', 'variant', 'configuration-filename', 'condition', 'current-block'])
        if key in seen:
            duplicates.append(item)
        else:
            seen.append(key)
            filtered.append(item)

    if duplicates:
        print(warning("** Some entries in the json is duplicated **"))
        for entry in duplicates:
            entry_duplicate_json = {
                'test-name': entry['test-name'],
                'variant': entry.get('variant', None),
                'configuration-filename': entry['configuration-filename'],
                'current-block': entry['current-block'],
                'condition': entry['condition'],
                'expected-result': entry['expected-result']
            }
            print(json.dumps(entry_duplicate_json, indent=4) + "\n", file=sys.__stdout__)

    return filtered


def parser_args_cli():
    # Parser arguments list which is supplied by the user
    parser = argparse.ArgumentParser(description='Parser arguments list which is supplied by the user')

    parser.add_argument('--export-dir', help='Directory to save exported results into', default=None)
    parser.add_argument('--no-parallel', help='Disable parallel running of tests', action='store_true')
    parser.add_argument('--no-parallel-for-no-alias', help='Disable parallel running of tests with no alias', action='store_true')
    parser.add_argument('--show-untested-test-modules', help='Flag to require show or do not show test modules which were untested', default='')
    parser.add_argument('--verbose', help='Print out details of unexpected failures/warnings including a template for the expected failures file, and print details of expected failures/warnings that do not happen', action='store_true')
    parser.add_argument('--expected-failures-file', help='Json configuration file name which records a list of expected failures/warnings', default='')
    parser.add_argument('--expected-skips-file', help='Json configuration file name which records a list of expected skipped tests', default='')
    parser.add_argument('params', nargs='+', help='List parameters contains test-plan-name and configuration-file to run all test plan. Syntax: <test-plan-name> <configuration-file> ...')

    return parser.parse_args()


def secondssince1970():
    return int(time.mktime(datetime.datetime.now().timetuple()))


def start_section(name, heading, collapsed=False):
    if "CI" not in os.environ:
        return
    sys.stdout.flush()
    sys.stderr.flush()
    # documentation: https://docs.gitlab.com/ee/ci/jobs/#custom-collapsible-sections
    if collapsed:
        name += "[collapsed=true]"
    print("\x1b[0Ksection_start:{}:{}\r\x1b[0K{}".format(secondssince1970(), name, heading), file=sys.__stdout__)


def end_section(name):
    if "CI" not in os.environ:
        return
    print("\x1b[0Ksection_end:{}:{}\r\x1b[0K".format(secondssince1970(), name,), file=sys.__stdout__)
    sys.stdout.flush()
    sys.stderr.flush()

async def run_test_plan_wrapper(plan_name, config_json, export_dir):
    result = await run_test_plan(plan_name, config_json, export_dir)
    if isinstance(result, list):
        results.extend(result)
    else:
        results.append(result)


async def main():
    global conformance
    global api_url_base
    global untested_test_modules
    global args

    dev_mode = 'CONFORMANCE_DEV_MODE' in os.environ

    if 'CONFORMANCE_SERVER' in os.environ:
        api_url_base = os.environ['CONFORMANCE_SERVER']
        if api_url_base == "":
            print("Error: run-test-plan.py: CONFORMANCE_SERVER in environment seems to be empty")
            sys.exit(1)
        if not api_url_base.endswith('/'):
            # make sure it ends in a / as the client tests assume that
            api_url_base += '/'
            os.environ['CONFORMANCE_SERVER'] = api_url_base
    else:
        # local development settings
        api_url_base = 'https://localhost.emobix.co.uk:8443/'
        dev_mode = True

        os.environ["CONFORMANCE_SERVER"] = api_url_base

    if dev_mode:
        token = None
    else:
        token = os.environ['CONFORMANCE_TOKEN']

    args = parser_args_cli()
    show_untested = args.show_untested_test_modules
    verbose = args.verbose
    params = args.params

    if len(params) % 2 == 1:
        print("Error: run-test-plan.py: must have even number of parameters")
        sys.exit(1)

    to_run = []
    while len(params) >= 2:
        to_run.append((params[0], params[1]))
        params = params[2:]

    verify_ssl = not dev_mode and not 'DISABLE_SSL_VERIFY' in os.environ
    conformance = Conformance(api_url_base, token, verify_ssl)

    for attempt in range(1, 12):
        try:
            all_test_modules_array = await conformance.get_all_test_modules()
            break
        except Exception as exc:
            # the server may not have finished starting yet; sleep & try again
            print('Failed to connect to conformance suite on attempt {}: {}'.format(attempt, exc))
            time.sleep(10)
    else:
        raise Exception("failed to connect to conformance suite")

    # convert the array into a dictionary with the testName as the key
    all_test_modules = {m['testName']: m for m in all_test_modules_array}
    untested_test_modules = sorted(all_test_modules.keys())

    expected_failures_list = []
    if args.expected_failures_file:
        expected_failures_list = load_expected_problems(args.expected_failures_file)

    expected_skips_list = []
    if args.expected_skips_file:
        expected_skips_list = load_expected_problems(args.expected_skips_file)

    global results
    results = []
    queues = {} # key is alias for test plan, or None
    workers = []
    for (plan_name, config_json) in to_run:
        with open(config_json) as f:
            json_config = f.read()
        parsed_config = json.loads(json_config)
        if args.no_parallel:
            # put all jobs into same queue
            alias = None
        else:
            alias = parsed_config["alias"] if "alias" in parsed_config else None
        if alias == "oidf-fapi-rp-test":
            # the RP against OP tests use the sandbox directory and hence the OP tests use this alias and hence need
            # to go into the same queue as the other tests that need this alias
            alias = "oidf-obbsb"
        if alias not in queues:
            queues[alias] = asyncio.Queue()
            parallel_jobs = 2 if alias == None and not args.no_parallel and not args.no_parallel_for_no_alias else 1
            print("Creating queue for "+str(alias)+" parallel="+str(parallel_jobs))
            workers.extend([asyncio.create_task(queue_worker(queues[alias])) for _ in range(parallel_jobs)])
        print("Adding {} {} to queue {}".format(plan_name, config_json, alias))
        queues[alias].put_nowait(run_test_plan_wrapper(plan_name, config_json, args.export_dir))

    for q in queues:
        print("plan: joining "+str(q))
        await queues[q].join()
    print("plan: queues done, cancelling workers")
    for worker in workers:
        worker.cancel()
    print("plan: workers cancelled, gathering")
    await asyncio.gather(*workers, return_exceptions=True)
    print("plan: workers gathered")

    await conformance.close_client()

    print("\n\nScript complete - results:")

    did_not_complete = False
    failed_plan_results = []
    for result in results:
        print('\n\nResults for {} with configuration {}:'.format(result['test_plan'], result['config_file']))
        plan_result = analyze_plan_results(result, expected_failures_list, expected_skips_list)
        show_plan_results(result, plan_result)
        plan_did_not_complete = plan_result['plan_did_not_complete']
        if plan_did_not_complete == 'NOT_COMPLETE':
            did_not_complete = True
        elif plan_did_not_complete == 'FAILURE_OR_WARNING':
            failed_plan_results.append(plan_result['detail_plan_result'])

    if did_not_complete:
        print(failure("** Exiting with failure - some tests did not run to completion **"))
        sys.exit(1)

    failed = False

    # analyze_plan_results will remove expected failures and skips from the list, so if
    # any remain at this point, they are unused and should be warned about.
    def is_unused(obj):
        return not '__used' in obj

    unused_expected_failures = list(filter(is_unused, expected_failures_list))
    if unused_expected_failures:
        print(failure("** Exiting with failure - some expected failures were not found in any test module of the system **"))
        if verbose:
            start_section("unused_expected_failures", "unused expected failures detail", True)
            for entry in unused_expected_failures:
                entry_invalid_json = {
                    'test-name': entry['test-name'],
                    'variant': entry.get('variant', None),
                    'configuration-filename': entry['configuration-filename'],
                    'current-block': entry['current-block'],
                    'condition': entry['condition'],
                    'expected-result': entry['expected-result']
                }
                print(json.dumps(entry_invalid_json, indent=4) + "\n", file=sys.__stdout__)
            end_section("unused_expected_failures")
        else:
            print("Rerun with --verbose to see details");
        failed = True

    unused_expected_skips = list(filter(is_unused, expected_skips_list))
    if unused_expected_skips:
        print(failure("** Exiting with failure - some expected skips were not found in any test module of the system **"))
        if verbose:
            start_section("unused_expected_skips", "unused expected skips detail", True)
            for entry in unused_expected_skips:
                entry_invalid_json = {
                    'test-name': entry['test-name'],
                    'variant': entry.get('variant', None),
                    'configuration-filename': entry['configuration-filename']
                }
                print(json.dumps(entry_invalid_json, indent=4) + "\n", file=sys.__stdout__)
            end_section("unused_expected_skips")
        else:
            print("Rerun with --verbose to see details");
        failed = True

    if failed_plan_results:
        if verbose:
            summary_unexpected_failures_all_test_plan(failed_plan_results)
            print(failure("** Exiting with failure - some test modules have unexpected condition failures/warnings **"))
        else:
            print(failure("** Exiting with failure - some test modules have unexpected condition failures/warnings - rerun with --verbose to see details **"))
        failed = True

    # filter untested list, as we don't currently have test environments for these
    for m in untested_test_modules[:]:
        if m in ignored_modules:
            untested_test_modules.remove(m)
            continue

        if all_test_modules[m]['profile'] in ['FAPI-RW-ID2']:
            # skip CI for FAPI ID2 as we don't have access to a server supporting ID2
            untested_test_modules.remove(m)
            continue

        if all_test_modules[m]['profile'] in ['OID4VCI']:
            # skip CI for credential issuance as we don't have access to a suitable server to use in the CI
            untested_test_modules.remove(m)
            continue

        if re.match(r'(oidcc-session-management-.*)', m):
            # The browser automation currently doesn't seem to work for the iframes/js these tests use
            untested_test_modules.remove(m)
            continue

        if m in ["oidcc-client-test-request-uri-signed-none", "oidcc-client-test-request-uri-signed-rs256"]:
            # It seems these are not currently tested by the CI; see:
            # https://gitlab.com/openid/conformance-suite/-/issues/840
            untested_test_modules.remove(m)
            continue

        #we don't have automated tests for OIDCC RP login/logout tests
        if re.match(r'(oidcc-client-test-.*logout.*)',m) or m == 'oidcc-client-test-session-management'\
            or m == 'oidcc-client-test-3rd-party-init-login':
            untested_test_modules.remove(m)
            continue

        # We're not requiring FAPI-CIBA RP testing for the negative tests for now, just the happy one
        if re.match(r'fapi-ciba-id1-client-.+-test',m):
            untested_test_modules.remove(m)
            continue

        client_test = re.match(r'fapi-rw-id2-client-.*', m) or \
                      re.match(r'fapi1-advanced-final-client-.*', m) or \
                      re.match(r'fapi2-security-profile-id2-client-.*', m) or \
                      re.match(r'oidcc-client-.*', m)
        ciba_op_test = re.match(r'fapi-ciba-id1.*', m)
        rp_initiated_logout = re.match(r'oidcc-.*-logout.*', m)
        ekyc_test = re.match(r'ekyc-server-', m)
        fapi1r = all_test_modules[m]['profile'] in ['FAPI-R']
        fapi1 = all_test_modules[m]['profile'] in ['FAPI1-Advanced-Final']
        fapi2 = all_test_modules[m]['profile'] in ['FAPI2-Security-Profile-ID2']
        brazildcr = re.match(r'.*brazil.*dcr.*', m)
        obuk = re.match(r'.*ensure-server-handles-non-matching-intent-id.*', m) or \
          re.match(r'.*test-essential-acr-sca-claim.*', m)

        if show_untested == 'client':
            # Only run client test, therefore ignore all server test
            if not client_test:
                untested_test_modules.remove(m)
                continue

            if fapi1 or fapi2:
                # no tests for FAPI1-Final / FAPI2 yet
                untested_test_modules.remove(m)
                continue
        elif show_untested == 'server-oidc-provider':
            # Only run server test, ignore all client/CIBA test, plus we don't run the FAPI tests against oidc provider
            if fapi1r or fapi1 or fapi2 or ciba_op_test or client_test or ekyc_test:
                untested_test_modules.remove(m)
                continue
        elif show_untested == 'server-authlete':
            # ignore all client/CIBA test, plus we don't run the rp initiated logout tests against Authlete
            # we've not yet setup fapi2 brazil dcr or uk test runs
            if client_test or ciba_op_test or rp_initiated_logout or ekyc_test or (fapi2 and (brazildcr or obuk)):
                untested_test_modules.remove(m)
                continue
        elif show_untested == 'ekyc':
            if not ekyc_test:
                untested_test_modules.remove(m)
                continue
        elif show_untested == 'all-except-logout':
            # we don't run the rp initiated logout tests against Authlete
            if rp_initiated_logout:
                untested_test_modules.remove(m)
                continue
        elif show_untested == 'ciba':
            # Only run server test, therefore ignore all ciba test
            if not ciba_op_test:
                untested_test_modules.remove(m)
                continue

    if show_untested and len(untested_test_modules) > 0:
        print(failure("** Exiting with failure - not all available modules were tested:"))
        for m in untested_test_modules:
            print('{}: {}'.format(all_test_modules[m]['profile'], m))
        failed = True

    if failed:
        sys.exit(1)

    print(success("All tests ran to completion. See above for any test condition failures."))
    sys.exit(0)

if __name__ == '__main__':
    asyncio.run(main())
