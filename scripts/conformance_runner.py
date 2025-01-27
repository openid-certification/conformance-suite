#!/usr/bin/env python3

import argparse
import json
import logging
import logging.config
import os
import queue
import sys
import threading
import time
from multiprocessing import Queue
from rich.logging import RichHandler
from rich.console import Console

from conformance_server import ConformanceServer
from workers.planrunner import PlanRunner
from workers.monitor import MonitorWorker
from workers.resultqueue import ResultQueue
from workers.taskqueue import TaskQueue, TestTask
from helpers import *
import fnmatch

# require to be executed before importing old code
the_sysout = sys.stdout
from run_test_plan import parser_args_cli, load_expected_problems, success, failure
from test_plan_parser import test_plan

sys.stdout = the_sysout

FORMAT = '%(asctime)s - %(levelname)s - %(name)s - %(message)s'
isTTY = False and sys.stdout.isatty()

args = parser_args_cli()

if isTTY:
    logging.basicConfig(format=FORMAT, level=logging.ERROR, handlers=[RichHandler()])
else:
    LOGGING_CONFIG = {
        "version": 1,
        "handlers": {
            "default": {
                "class": "logging.StreamHandler",
                "formatter": "http",
                "stream": "ext://sys.stderr"
            },
            "rich": {
                "class": "rich.logging.RichHandler",
                "console": Console(width=250),
                "markup": True,
                "show_path": False,
                "omit_repeated_times": False
            }
        },
        "formatters": {
            "http": {
                "format": "%(levelname)s [%(asctime)s] %(name)s - %(message)s",
                "datefmt": "%Y-%m-%d %H:%M:%S",
            }
        },
        'loggers': {
            'httpx': {
                'handlers': ['default'],
                'level': 'ERROR',
            },
            'httpcore': {
                'handlers': ['default'],
                'level': 'ERROR',
            },
            'workers.taskqueue': {
                'level': 'DEBUG' if args.verbose else 'INFO',
            },
            'workers.monitor': {
                'level': 'DEBUG' if args.verbose else 'INFO',
            },
            'workers.planrunner': {
                'level': 'DEBUG' if args.verbose else 'INFO',
            }
        },
        'root': {
            'handlers': ['rich'],
            'level': 'DEBUG' if args.verbose else 'INFO',
            'format': "%(message)s"
        }
    }

    logging.config.dictConfig(LOGGING_CONFIG)

logger = logging.getLogger(__name__)


def split_params(param):
    params = param.split(' ')
    to_return = []
    i = 0
    while i < len(params):
        j = i + 1
        while params[j].startswith("id_token"):
            j = j + 1
        first = " ".join(params[i:j])
        i = j
        second = params[i]
        i = i + 1
        to_return.append((first, second))

    return to_return


def main():
    show_untested = args.show_untested_test_modules
    verbose = args.verbose
    params = args.params

    dev_mode = 'CONFORMANCE_DEV_MODE' in os.environ

    if api_url_base := os.getenv('CONFORMANCE_SERVER'):
        if not api_url_base.endswith('/'):
            # make sure it ends in a / as the client tests assume that
            api_url_base += '/'
        if mtls_url_base := os.getenv('CONFORMANCE_SERVER_MTLS'):
            if not mtls_url_base.endswith('/'):
                # make sure it ends in a / as the client tests assume that
                mtls_url_base += '/'
        else:
            mtls_url_base = api_url_base
    else:
        # local development settings
        api_url_base = 'https://localhost.emobix.co.uk:8443/'
        mtls_url_base = 'https://localhost.emobix.co.uk:8444/'
        dev_mode = True

    os.environ["CONFORMANCE_SERVER"] = api_url_base
    os.environ["CONFORMANCE_SERVER_MTLS"] = mtls_url_base

    if dev_mode:
        token = None
    else:
        token = os.environ['CONFORMANCE_TOKEN']

    logger.info(f"server with urls: {api_url_base} and {mtls_url_base} with devmode {dev_mode} and token {token}")

    verify_ssl = not dev_mode and not 'DISABLE_SSL_VERIFY' in os.environ
    conformance_server = ConformanceServer(api_url_base, token, verify_ssl)
    client_certs = load_client_certs()

    to_run = []

    cmd_line = ' '.join(params)
    for tokens, start, end  in test_plan.scan_string(cmd_line):
        test = cmd_line[start:end]
        a_test = tokens[0]
        a_test["src"] = test
        to_run.append((a_test, a_test["test"]["config_file"]))

    expected_failures_list = []
    if args.expected_failures_file:
        logger.info(f"loading expected failures from: {args.expected_failures_file}")
        expected_failures_list = load_expected_problems(args.expected_failures_file)

    parser = TestConfigParser(client_certs=client_certs, baseurl=api_url_base, baseurlmtls=mtls_url_base)

    taskQueue = TaskQueue()
    for (plan_config, plan_config_file_name) in to_run:

        (json_config, parsed_config) = parser.parse(plan_config_file_name)
        alias = parsed_config["alias"] if "alias" in parsed_config else None
        if alias == "oidf-fapi-rp-test":
            alias = "oidf-obbsb"
        task = TestTask(alias, plan_config, plan_config_file_name, json_config, parsed_config)
        logger.debug(f"alias {alias} -> scheduling {plan_config} using config file {plan_config_file_name}")
        taskQueue.add_task(task)

    taskQueue.fill_priority_queue()
    taskQueue.dump_state_logger()

    monitor = MonitorWorker(isTTY=isTTY, task_queue=taskQueue)

    runners = []
    result_queue = ResultQueue(expected_failures_list)
    runners.extend([PlanRunner(task_queue=taskQueue,
                               status_queue=monitor,
                               conformance_server=conformance_server,
                               api_url_base=api_url_base,
                               output_dir=args.export_dir,
                               config_parser=parser,
                               result_queue=result_queue) for _ in range(8)])
    taskQueue.join(not isTTY)
    # all the tests are executed and we need to present the details
    res = result_queue.get_task_results()

    tests_to_retry = res["tests_to_retry"]
    del res["tests_to_retry"]
    for key in res.keys():
        val = res[key]
        if val["failed"] > 0:
            continue

        # log all the errors
        logger.info(success(f"test {key} - {val["succeeded"]} succeeded, {val["expected_failed"]} expected failure "))

    first_error = True
    for key in res.keys():
        val = res[key]
        if val["failed"] == 0:
            continue
        if first_error:
            first_error = False
            logger.info(failure(f"THESE TESTS HAVE FAILED:"))
        logger.info(failure(f"test {key} - {val["failed"]} failures,  {val["succeeded"]} succeeded, {val["expected_failed"]} expected failure "))
        logger.info("Check out the test links below")
        for url in val["failed_tests_links"]:
            logger.info(url)

    if not first_error:
        logger.info(failure(f"creating retry file with {len(tests_to_retry)} elements"))
        for key in tests_to_retry:
            print(f"{key}")

    if tests_to_retry :
        sys.exit(1)

def load_client_certs():
    client_certs = {}
    key_directory = os.path.dirname(os.path.abspath(__file__)) + '/certs-keys/'
    key_files = os.listdir(key_directory)

    for a_file in key_files:
        logger.info(f"Loading certificate: {a_file}")
        with open(key_directory + a_file) as f:
            cnt = f.read()
            client_certs[a_file] = cnt.replace("\n", " ")
    return client_certs

def expected_failure(expected_failures, testname, variant, configuration_filename, test_result):
    for expected_failure in expected_failures:
        if expected_failure["test-name"] == testname and fnmatch.fnmatch(configuration_filename, expected_failure['configuration-filename']):

            if not expected_failure["variant"] == "*":
                # if the expected failure variant accept anything, we don't need to compare the test variants
                for key, val in expected_failure["variant"].items():
                    if key not in variant:
                        # the key declared
                        continue
                    if not variant[key] == val:
                        continue

            log = test_result["logs"]

            # Filter the starting of blocks and entries with a result
            failed_entries = [x for x in log if 'startBlock' in x or
                              ('result' in x and not (x['result'] == 'INFO' or x['result'] == 'SUCCESS'))]
            currentBlock = ""
            for line in failed_entries:
                if 'startBlock' in line:
                    currentBlock = line['msg']
                elif currentBlock == expected_failure['current-block']:
                    if 'result' in line and 'src' in line and line['src'] == expected_failure['condition']:
                        # the condition and the block were founded
                        if ((expected_failure['expected-result'] == 'warning' and line['result'] == "WARNING") or
                                (expected_failure['expected-result'] == 'failure' and line['result'] == "FAILURE")):
                            return True
    return False

if __name__ == "__main__":
    main()
