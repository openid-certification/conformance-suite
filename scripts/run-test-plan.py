#!/usr/bin/env python
#
# Author: Joseph Heenan

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import re
import sys
import time

import requests

from conformance import Conformance

import sys

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

def run_test_plan(test_plan, config_file):
    print("Running plan '{}' with configuration file '{}'".format(test_plan, config_file))
    with open(config_file) as f:
        json_config = f.read()
    test_plan_info = conformance.create_test_plan(test_plan, json_config)
    plan_id = test_plan_info['id']
    plan_modules = test_plan_info['modules']
    test_ids = {}  # key is module name
    test_time_taken = {}  # key is module_id
    overall_start_time = time.time()
    print('Created test plan, new id: {}'.format(plan_id))
    print('{}plan-detail.html?plan={}'.format(api_url_base, plan_id))
    print('{:d} modules to test:\n{}\n'.format(len(plan_modules), '\n'.join(plan_modules)))
    for module in plan_modules:
        test_start_time = time.time()
        module_id = ''

        try:
            print('Running test module: {}'.format(module))
            test_module_info = conformance.create_test_from_plan(plan_id, module)
            module_id = test_module_info['id']
            test_ids[module] = module_id
            print('Created test module, new id: {}'.format(module_id))
            print('{}log-detail.html?log={}'.format(api_url_base, module_id))

            conformance.wait_for_state(module_id, ["FINISHED"])

        except Exception as e:
            print('Exception: Test {} failed to run to completion: {}'.format(module, e))
        if module_id != '':
            test_time_taken[module_id] = time.time() - test_start_time
    overall_time = time.time() - overall_start_time
    print('\n\n')
    return {
        'test_plan': test_plan,
        'config_file': config_file,
        'plan_id': plan_id,
        'plan_modules': plan_modules,
        'test_ids': test_ids,
        'test_time_taken': test_time_taken,
        'overall_time': overall_time
    }


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


def failure(text):
    return color(text, bold=True, fg_red=True)


def warning(text):
    return color(text, bold=True, fg_orange=True)


def success(text):
    return color(text, fg_green=True)


# Returns 'did_not_complete', ie. True if any test failed to run to completion
def show_plan_results(plan_result):
    plan_id = plan_result['plan_id']
    plan_modules = plan_result['plan_modules']
    test_ids = plan_result['test_ids']
    test_time_taken = plan_result['test_time_taken']
    overall_time = plan_result['overall_time']

    warnings_overall = []
    failures_overall = []
    incomplete = 0
    successful_conditions = 0
    print('\n\nResults for {} with configuration {}:'.format(plan_result['test_plan'], plan_result['config_file']))

    for module in plan_modules:
        if module not in test_ids:
            print(failure('Test {} did not run'.format(module)))
            continue
        module_id = test_ids[module]

        info = conformance.get_module_info(module_id)

        logs = conformance.get_test_log(module_id)
        counts = {'SUCCESS': 0, 'WARNING': 0, 'FAILURE': 0}
        failures = []
        warnings = []

        if module in untested_test_modules:
            untested_test_modules.remove(module)

        if info['status'] != 'FINISHED':
            incomplete += 1
        if 'result' not in info:
            info['result'] = 'UNKNOWN'

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

        if module_id in test_time_taken:
            test_time = test_time_taken[module_id]
        else:
            test_time = -1
        if info['result'] == 'PASSED':
            result_coloured = success(info['result'])
        elif info['result'] == 'WARNING':
            result_coloured = warning(info['result'])
        else:
            result_coloured = failure(info['result'])
        print('Test {} {} {} - result {}. {:d} log entries - {:d} SUCCESS {:d} FAILURE, {:d} WARNING, {:.1f} seconds'.
              format(module, module_id, info['status'], result_coloured, len(logs),
                     counts['SUCCESS'], counts['FAILURE'], counts['WARNING'], test_time))
        if len(failures) > 0:
            print(failure("Failures: {}".format(', '.join(failures))))
        if len(warnings) > 0:
            print(warning("Warnings: {}".format(', '.join(warnings))))
        failures_overall.extend(failures)
        warnings_overall.extend(warnings)
        successful_conditions += counts['SUCCESS']
    print(
        '\nOverall totals: ran {:d} test modules. '
        'Conditions: {:d} successes, {:d} failures, {:d} warnings. {:.1f} seconds'.
        format(len(test_ids), successful_conditions, len(failures_overall), len(warnings_overall), overall_time))
    print('\n{}plan-detail.html?plan={}\n'.format(api_url_base, plan_id))
    if len(test_ids) != len(plan_modules):
        print(failure("** NOT ALL TESTS FROM PLAN WERE RUN **"))
        return True
    if incomplete != 0:
        print(failure("** {:d} TEST MODULES DID NOT RUN TO COMPLETION **".format(incomplete)))
        return True
    return False


if __name__ == '__main__':
    requests_session = requests.Session()

    dev_mode = False
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
        dev_mode = True

    if dev_mode or 'DISABLE_SSL_VERIFY' in os.environ:
        # disable https certificate validation
        requests_session.verify = False
        import urllib3

        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

    if len(sys.argv) < 3:
        print("Syntax: run-test-plan.py <test-plan-name> <configuration-file> ...")
        sys.exit(1)

    args = sys.argv[1:]
    show_untested = False
    if args[0] == '--show-untested-test-modules':
        show_untested = True
        args = args[1:]
    to_run = []
    while len(args) >= 2:
        to_run.append((args[0], args[1]))
        args = args[2:]

    if len(args) != 0:
        print("Error: run-test-plan.py: must have even number of parameters")
        sys.exit(1)

    conformance = Conformance(api_url_base, token_endpoint, requests_session)

    for attempt in range(1, 12):
        try:
            conformance.authorise(client_id, client_secret)
            break
        except Exception as exc:
            # the server may not have finished starting yet; sleep & try again
            print('Failed to connect to microauth on attempt {}: {}'.format(attempt, exc))
            time.sleep(10)
    else:
        raise Exception("failed to connect to microauth")

    for attempt in range(1, 12):
        try:
            all_test_modules_array = conformance.get_all_test_modules()
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

    results = []
    for (plan_name, config_json) in to_run:
        result = run_test_plan(plan_name, config_json)
        results.append(result)

    print("\n\nScript complete - results:")

    did_not_complete = False
    for result in results:
        plan_did_not_complete = show_plan_results(result)
        if plan_did_not_complete:
            did_not_complete = True

    if did_not_complete:
        print(failure("** Exiting with failure - some tests did not run to completion"))
        sys.exit(1)

    # filter untested list, as we don't currently have test environments for these
    for m in untested_test_modules[:]:
        if all_test_modules[m]['profile'] in ['SAMPLE', 'HEART']:
            untested_test_modules.remove(m)
            continue

        if re.match(r'fapi-ob-client-.*', m):
            # see https://gitlab.com/fintechlabs/fapi-conformance-suite/issues/351
            untested_test_modules.remove(m)
            continue

        if re.match(r'ob-deprecated-.*code-with-mtls', m):
            # we don't have a test environment that supports oauth-mtls and the code
            # response type
            untested_test_modules.remove(m)
            continue

    if show_untested and len(untested_test_modules) > 0:
        print(failure("** Exiting with failure - not all available modules were tested:"))
        for m in untested_test_modules:
            print('{}: {}'.format(all_test_modules[m]['profile'], m))
        sys.exit(1)

    print(success("All tests ran to completion. See above for any test condition failures."))
    sys.exit(0)
