#!/usr/bin/env python
#
# Author: Joseph Heenan

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import traceback
import os
import sys

import requests

from conformance import Conformance

requests_session = requests.Session()

if 'CONFORMANCE_SERVER' in os.environ:
    api_url_base = os.environ['CONFORMANCE_SERVER']
    token_endpoint = os.environ['CONFORMANCE_TOKEN_ENDPOINT']
    client_id = os.environ['CONFORMANCE_CLIENT_ID']
    client_secret = os.environ['CONFORMANCE_CLIENT_SECRET']
else:
    # local development settings
    api_url_base = 'https://localhost:8443/'
    token_endpoint = 'http://localhost:9001/token'
    client_id = 'oauth-client-1'
    client_secret = 'oauth-client-secret-1'

    # disable https certificate validation
    requests_session.verify = False
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

conformance = Conformance(api_url_base, token_endpoint, requests_session)
conformance.authorise(client_id, client_secret)

if len(sys.argv) != 3:
    print("Syntax: run-test-plan.py <test-plan-name> <configuration-file>")
    sys.exit(0)

test_plan = sys.argv[1]
config_file = sys.argv[2]

print("Running plan '{}' with configuration file '{}'", test_plan, config_file)

with open(config_file) as f:
    json_config = f.read()

test_plan_info = conformance.create_test_plan(test_plan, json_config)

plan_id = test_plan_info['id']
plan_modules = test_plan_info['modules']
test_ids = {}

print('Created test plan, new id: {}'.format(plan_id))
print('{}plan-detail.html?plan={}'.format(api_url_base, plan_id))
print('{:d} modules to test:\n{}\n'.format(len(plan_modules), '\n'.join(plan_modules)))
for module in plan_modules:
    try:
        print('Running test module: {}'.format(module))
        test_module_info = conformance.create_test(plan_id, module, json_config)
        module_id = test_module_info['id']
        test_ids[module] = module_id
        print('Created test module, new id: {}'.format(module_id))
        print('{}log-detail.html?log={}'.format(api_url_base, module_id))

        state = conformance.wait_for_state(module_id, ["CONFIGURED", "FINISHED"])

        if state == "CONFIGURED":
            print('Starting test')
            x = conformance.start_test(module_id)

            conformance.wait_for_state(module_id, ["FINISHED"])

    except Exception as e:
        print('Test {} failed to run to completion:'.format(module))
        traceback.print_exc()

print("\n\nScript complete - results:\n")

warnings_overall = []
failures_overall = []
for module in plan_modules:
    if module not in test_ids:
        print('Test {} did not run'.format(module))
        continue
    module_id = test_ids[module]

    info = conformance.get_module_info(module_id)

    logs = conformance.get_test_log(module_id)
    counts = {'WARNING': 0, 'FAILURE': 0}
    failures = []
    warnings = []
    for log_entry in logs:
        if 'result' not in log_entry:
            continue
        log_result = log_entry['result']  # contains WARNING/FAILURE/INFO/etc
        if log_result in counts:
            counts[log_result] += 1
            if log_result == 'FAILURE':
                failures.append(log_entry['src'])
            if log_result == 'WARNING':
                warnings.append(log_entry['src'])

    print('Test {} {} {} - result {}. {:d} log entries - {:d} FAILURE, {:d} WARNING'.
          format(module, module_id, info['status'], info['result'], len(logs),
                 counts['FAILURE'], counts['WARNING']))
    if len(failures) > 0:
        print("Failures: {}".format(', '.join(failures)))
    if len(warnings) > 0:
        print("Warnings: {}".format(', '.join(warnings)))
    failures_overall.extend(failures)
    warnings_overall.extend(warnings)


print('\nOverall totals: run {:d} test modules, {:d} failures, {:d} warnings\n'.format(
    len(test_ids), len(failures_overall), len(warnings_overall)))

print('\nResults are at: {}plan-detail.html?plan={}\n'.format(api_url_base, plan_id))
if len(test_ids) != len(plan_modules):
    print("** NOT ALL TESTS WERE RUN **")
