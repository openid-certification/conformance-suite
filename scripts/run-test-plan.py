#!/usr/bin/env python
#
# Author: Joseph Heenan

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import traceback

import requests

from conformance import Conformance

# api_token = 'your_token_goes_here'

api_url_base = 'https://localhost.emobix.co.uk/'
# FIXME: need authentication before we can do this
# api_url_base = 'https://fintechlabs-fapi-conformance-suite-staging.fintechlabs.io/'


requests_session = requests.Session()
requests_session.verify = False  # FIXME enable for live system
import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
# headers = {'Content-Type': 'application/json',
#           'Authorization': 'Bearer {0}'.format(api_token)} # FIXME: need token for live system

headers = {'Content-Type': 'application/json'}
requests_session.headers = headers

conformance = Conformance(api_url_base, requests_session)

test_plan = 'ob-code-id-token-with-private-key-and-matls-test-plan'
#test_plan = 'ob-code-with-private-key-and-matls-test-plan'
with file('config.json') as f:
    json_config = f.read()

test_plan_info = conformance.create_test_plan(test_plan, json_config)

plan_id = test_plan_info['id']
plan_modules = test_plan_info['modules']
test_ids = {}

print('Created test plan, new id: {}'.format(plan_id))
print('{:d} modules to test:\n{}\n'.format(len(plan_modules), '\n'.join(plan_modules)))
for module in plan_modules:
    try:
        print('Running test module: {}'.format(module))
        test_module_info = conformance.create_test(plan_id, module, json_config)
        module_id = test_module_info['id']
        test_ids[module] = module_id
        print('Created test module, new id: {}'.format(module_id))

        conformance.wait_for_state(module_id, "CONFIGURED")

        print('Starting test')
        x = conformance.start_test(module_id)

        conformance.wait_for_state(module_id, "FINISHED")

    except Exception as e:
        print('Test {} failed to run to completion:'.format(module))
        traceback.print_exc()

print("\n\nScript complete - results:\n")

for module, module_id in test_ids.items():
    info = conformance.get_module_info(module_id)

    logs = conformance.get_test_log(module_id)
    counts = {'WARNING': 0, 'FAILURE': 0}
    for l in logs:
        if 'result' not in l:
            continue
        lresult = l['result']
        if lresult not in counts:
            counts[lresult] = 0
        counts[lresult] += 1
    print('Test {} {} {} - result {}. {:d} log entries - {:d} FAILURE, {:d} WARNING'.
          format(module, module_id, info['status'], info['result'], len(logs), counts['FAILURE'], counts['WARNING']))

if len(test_ids) != len(plan_modules):
    print("** NOT ALL TESTS WERE RUN **")
