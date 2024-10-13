#!/usr/bin/env python3

import argparse
import json
import logging
import os
import queue
import sys
import threading
import time
from multiprocessing import Queue

from conformance import Conformance
from workers.planrunner import PlanRunner
from workers.monitor import MonitorWorker
from workers.taskqueue import TaskQueue, TestTask

# require to be executed before importing old code
the_sysout = sys.stdout
from run_test_plan import parser_args_cli, load_expected_problems

sys.stdout = the_sysout

plans = ["1", "2", "3", "4", "5", "6"]

FORMAT = '%(asctime)s - %(levelname)s - %(name)s - %(message)s'
isTTY = False and sys.stdout.isatty()

if isTTY:
    logging.basicConfig(format=FORMAT, level=logging.ERROR)
else:
    logging.basicConfig(format=FORMAT, level=logging.WARNING)
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
    args = parser_args_cli()
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

    logger.info("server with urls: %(base)s and %(mtls)s with devmode %(devmode)s and token %(token)s",
                {'base': api_url_base,
                 'mtls': mtls_url_base,
                 'devmode': dev_mode,
                 'token': token})

    verify_ssl = not dev_mode and not 'DISABLE_SSL_VERIFY' in os.environ
    conformance_server = Conformance(api_url_base, token, verify_ssl)
    client_certs = loadClientCerts()

    to_run = split_params(' '.join(params).replace('\\', ''))
    if len(params) % 2 == 1:
        print("Error: run-test-plan.py: must have even number of parameters")
        sys.exit(1)

    expected_failures_list = []
    if args.expected_failures_file:
        expected_failures_list = load_expected_problems(args.expected_failures_file)

    expected_skips_list = []
    if args.expected_skips_file:
        expected_skips_list = load_expected_problems(args.expected_skips_file)

    taskQueue = TaskQueue()
    for (plan_config, plan_config_file_name) in to_run:
        with open(plan_config_file_name) as f:
            json_config = f.read()
        json_config = json_config.replace('{BASEURL}', os.environ['CONFORMANCE_SERVER'])
        json_config = json_config.replace('{BASEURLMTLS}', os.environ['CONFORMANCE_SERVER_MTLS'])

        for k,v in client_certs.items():
            json_config = json_config.replace('{'+ k + '}', v)

        parsed_config = json.loads(json_config)
        alias = parsed_config["alias"] if "alias" in parsed_config else None
        if alias == "oidf-fapi-rp-test":
            alias = "oidf-obbsb"
        task = TestTask(alias, plan_config, plan_config_file_name, json_config, parsed_config)
        taskQueue.add_task(task)

    taskQueue.fill_priority_queue()

    monitor = MonitorWorker(isTTY=isTTY, task_queue=taskQueue)

    runners = []

    runners.extend([PlanRunner(task_queue=taskQueue,
                               status_queue=monitor,
                               conformance_server = conformance_server,
                               api_url_base=api_url_base,
                               output_dir= args.export_dir) for _ in range(8)])
    taskQueue.join(not isTTY)

    #testing it now

    # for key, aqueue in queues.items():
    #     logger.info("Queue %(key)s has size %(size)d", {"key": key, "size": aqueue.qsize()})

    # status_queue = Queue()
    #
    # current_threads = []
    # for i in plans:
    #     t = PlanTestWorker(planid=i, status_queue=status_queue)
    #     t.start()
    #     current_threads.append(t)
    #
    # monitor = MonitorWorker(queue=status_queue)
    # monitor.start()
    #
    # for t in current_threads:
    #     t.join()
    #
    # status_queue.put("exit")


def loadClientCerts():
    client_certs = {}
    key_directory = os.path.dirname(os.path.abspath(__file__)) + '/certs-keys/'
    key_files = os.listdir(key_directory)

    for a_file in key_files:
        print('Loading ' + a_file)
        with open(key_directory + a_file) as f:
            cnt = f.read()
            client_certs[a_file] = cnt.replace("\n", " ")
    return client_certs


if __name__ == "__main__":
    main()
