#!/usr/bin/env python

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import re
import sys
import time
import subprocess

import requests
import json
import argparse

from conformance import Conformance

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

            state = conformance.wait_for_state(module_id, ["WAITING", "FINISHED"])

            if state == "WAITING":
                # If it's a client test, we need to run the client
                if re.match(r'(fapi-rw-id2-client-.*)', module):
                    os.putenv('CLIENTTESTMODE', 'fapi-rw')
                    subprocess.call(["npm", "run", "client"], cwd="./sample-openbanking-client-nodejs")

                if re.match(r'(fapi-rw-id2-ob-client-.*)', module):
                    os.putenv('CLIENTTESTMODE', 'fapi-ob')
                    subprocess.call(["npm", "run", "client"], cwd="./sample-openbanking-client-nodejs")

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


# Returns object contains 'plan_did_not_complete' and 'detail_plan_result', ie.
# 'plan_did_not_complete' = NOT_COMPLETE
#     some test modules did not run complete
# 'plan_did_not_complete' = FAILURE_OR_WARNING
#     if any test failed to run to completion
#     if condition failure/warning occurs and isn't listed in the config json file
#     if failure/warning is expected and doesn't occur
# 'plan_did_not_complete' = COMPLETE
#     all test modules run complete
def show_plan_results(plan_result, expected_failures_json):
    plan_id = plan_result['plan_id']
    plan_modules = plan_result['plan_modules']
    test_ids = plan_result['test_ids']
    test_time_taken = plan_result['test_time_taken']
    overall_time = plan_result['overall_time']

    incomplete = 0
    successful_conditions = 0
    overall_test_results = []

    number_of_failures = 0
    number_of_warnings = 0

    counts_unexpected = {
        'EXPECTED_FAILURES': 0,
        'UNEXPECTED_FAILURES': 0,
        'EXPECTED_FAILURES_NOT_HAPPEN': 0,
        'EXPECTED_WARNINGS': 0,
        'UNEXPECTED_WARNINGS': 0,
        'EXPECTED_WARNINGS_NOT_HAPPEN': 0
    }

    print('\n\nResults for {} with configuration {}:'.format(plan_result['test_plan'], plan_result['config_file']))

    for module in plan_modules:
        if module not in test_ids:
            print(failure('Test {} did not run'.format(module)))
            continue
        module_id = test_ids[module]
        info = conformance.get_module_info(module_id)
        logs = conformance.get_test_log(module_id)

        if module in untested_test_modules:
            untested_test_modules.remove(module)

        if info['status'] != 'FINISHED':
            incomplete += 1
        if 'result' not in info:
            info['result'] = 'UNKNOWN'

        test_name = info['testName']
        result = analyze_result_logs(test_name, logs, expected_failures_json, counts_unexpected)

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
              format(module, module_id, info['status'], result_coloured, len(logs),
                     counts['SUCCESS'], counts['FAILURE'], counts['WARNING'], test_time))

        test_result = summary_unexpected_failures_test_module(result, test_name, module_id)
        if test_result:
            overall_test_results.append(test_result)

        successful_conditions += counts['SUCCESS']
        number_of_failures += counts['FAILURE']
        number_of_warnings += counts['WARNING']

    print(
        '\nOverall totals: ran {:d} test modules. '
        'Conditions: {:d} successes, {:d} failures, {:d} warnings. {:.1f} seconds'.
        format(len(test_ids), successful_conditions, number_of_failures, number_of_warnings, overall_time))
    print('\n{}plan-detail.html?plan={}\n'.format(api_url_base, plan_id))

    if len(test_ids) != len(plan_modules):
        print(failure("** NOT ALL TESTS FROM PLAN WERE RUN **"))
        return {'plan_did_not_complete': 'NOT_COMPLETE', 'detail_plan_result': {}}

    if incomplete != 0:
        print(failure("** {:d} TEST MODULES DID NOT RUN TO COMPLETION **".format(incomplete)))
        return {'plan_did_not_complete': 'NOT_COMPLETE', 'detail_plan_result': {}}

    has_failure_or_warning = False
    if counts_unexpected['UNEXPECTED_FAILURES'] > 0:
        print(failure("** SOME TEST MODULES HAVE CONDITION UNEXPECTED FAILURES **"))
        has_failure_or_warning = True

    if  counts_unexpected['UNEXPECTED_WARNINGS'] > 0:
        print(failure("** SOME TEST MODULES HAVE CONDITION UNEXPECTED WARNINGS **"))
        has_failure_or_warning = True

    if counts_unexpected['EXPECTED_FAILURES_NOT_HAPPEN'] > 0:
        print(failure("** SOME TEST MODULES HAVE CONDITION EXPECTED FAILURE DID NOT HAPPEN **"))
        has_failure_or_warning = True

    if counts_unexpected['EXPECTED_WARNINGS_NOT_HAPPEN'] > 0:
        print(failure("** SOME TEST MODULES HAVE CONDITION EXPECTED WARNING DID NOT HAPPEN **"))
        has_failure_or_warning = True

    detail_plan_result = {
        'plan_name': plan_result['test_plan'],
        'plan_config_file': plan_result['config_file'],
        'overall_test_results': overall_test_results,
        'counts_unexpected': counts_unexpected
    }
    if has_failure_or_warning:
        return {'plan_did_not_complete': 'FAILURE_OR_WARNING', 'detail_plan_result': detail_plan_result}

    return {'plan_did_not_complete': 'COMPLETE', 'detail_plan_result': {}}

# Analyze all log of a test module and return object contains:
#   'expected_failures': list all expected failures condition
#   'unexpected_failures': list all unexpected failures condition
#   'expected_failures_did_not_happen': list all expected failures condition did not happen
#   'expected_warnings': list all expected warnings condition
#   'unexpected_warnings': list all unexpected warnings condition
#   'expected_warnings_did_not_happen': list all expected warnings condition did not happen
#   'counts': contains number of success condition, number of warning condition and number of failure condition
def analyze_result_logs(test_name, logs, expected_failures_list, counts_unexpected):
    counts = {'SUCCESS': 0, 'WARNING': 0, 'FAILURE': 0}
    expected_failures = []
    unexpected_failures = []
    expected_failures_did_not_happen = []

    expected_warnings = []
    unexpected_warnings = []
    expected_warnings_did_not_happen = []

    blockMsg = ''
    for log_entry in logs:
        if ('startBlock' in log_entry and log_entry['startBlock'] == True and log_entry['src'] == '-START-BLOCK-'):
            blockMsg = log_entry['msg']
            continue
        if 'result' not in log_entry:
            continue

        log_result = log_entry['result']  # contains WARNING/FAILURE/INFO/etc
        if log_result in counts:
            counts[log_result] += 1
            log_entry_exist_in_expected_list = False
            for expected_failure_obj in expected_failures_list:
                if (expected_failure_obj['test-name'] == test_name and expected_failure_obj['condition'] == log_entry['src'] and expected_failure_obj['current-block'] == blockMsg):

                    if (log_result == 'FAILURE' and expected_failure_obj['expected-result'] == 'failure'):
                        expected_failures.append({'current_block': blockMsg, 'src': log_entry['src']})
                        counts_unexpected['EXPECTED_FAILURES'] += 1

                    elif (log_result == 'WARNING' and expected_failure_obj['expected-result'] == 'warning'):
                        expected_warnings.append({'current_block': blockMsg, 'src': log_entry['src']})
                        counts_unexpected['EXPECTED_WARNINGS'] += 1

                    else:
                        if expected_failure_obj['expected-result'] == 'failure':
                            expected_failures_did_not_happen.append({'current_block': blockMsg, 'src': log_entry['src']})
                            counts_unexpected['EXPECTED_FAILURES_NOT_HAPPEN'] += 1

                        if expected_failure_obj['expected-result'] == 'warning':
                            expected_warnings_did_not_happen.append({'current_block': blockMsg, 'src': log_entry['src']})
                            counts_unexpected['EXPECTED_WARNINGS_NOT_HAPPEN'] += 1

                    log_entry_exist_in_expected_list = True
                    break

            if  log_entry_exist_in_expected_list == False:
                if log_result == 'FAILURE':
                    unexpected_failures.append({'current_block': blockMsg, 'src': log_entry['src']})
                    counts_unexpected['UNEXPECTED_FAILURES'] += 1

                if log_result == 'WARNING':
                    unexpected_warnings.append({'current_block': blockMsg, 'src': log_entry['src']})
                    counts_unexpected['UNEXPECTED_WARNINGS'] += 1

    return {
        'expected_failures': expected_failures,
        'unexpected_failures': unexpected_failures,
        'expected_failures_did_not_happen': expected_failures_did_not_happen,
        'expected_warnings': expected_warnings,
        'unexpected_warnings': unexpected_warnings,
        'expected_warnings_did_not_happen': expected_warnings_did_not_happen,
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

    has_unexpected_failures = False
    if len(expected_failures) > 0:
        print(failure("Expected failure: "))
        print_failure_warning(expected_failures, 'failure', '\t')

    if len(unexpected_failures) > 0:
        has_unexpected_failures = True
        print(failure("Unexpected failure: "))
        print_failure_warning(unexpected_failures, 'failure', '\t')

    if len(expected_failures_did_not_happen) > 0:
        has_unexpected_failures = True
        print(failure("Expected failure dit not happen: "))
        print_failure_warning(expected_failures_did_not_happen, 'failure', '\t')

    if len(expected_warnings) > 0:
        print(warning("Expected warning: "))
        print_failure_warning(expected_warnings, 'warning', '\t')

    if len(unexpected_warnings) > 0:
        has_unexpected_failures = True
        print(warning("Unexpected warning: "))
        print_failure_warning(unexpected_warnings, 'warning', '\t')

    if len(expected_warnings_did_not_happen) > 0:
        has_unexpected_failures = True
        print(warning("Expected warning did not happen: "))
        print_failure_warning(expected_warnings_did_not_happen, 'warning', '\t')

    test_result = {}
    if has_unexpected_failures:
        log_detail_link = '{}log-detail.html?log={}'.format(api_url_base, module_id)
        test_result = {'test_name': test_name, 'log_detail_link': log_detail_link, 'test_result': result}

    return test_result


# Output all unexpected failures for all test plan at the end of run-test-plan.py file
# return a flag 'has_unexpected_failures', ie.
#   'has_unexpected_failures' = true: existed unexpected failures/warnings in some test plan
#   'has_unexpected_failures' = false: not existed unexpected failures/warnings in all test plan
def summary_unexpected_failures_all_test_plan(detail_plan_results):
    has_unexpected_failures = False
    for detail_plan_result in detail_plan_results:
        if detail_plan_result:
            has_unexpected_failures = True
            print(failure('{} - {}: '.format(detail_plan_result['plan_name'], detail_plan_result['plan_config_file'])))
            overall_test_results = detail_plan_result['overall_test_results']
            counts_unexpected = detail_plan_result['counts_unexpected']

            if counts_unexpected['UNEXPECTED_FAILURES'] > 0:
                print(failure('\tUnexpected failure: '))
                output_summary_test_plan_by_unexpected_type(overall_test_results, 'unexpected_failures', 'failure')

            if counts_unexpected['EXPECTED_FAILURES_NOT_HAPPEN'] > 0:
                print(failure('\tExpected failure did not happen: '))
                output_summary_test_plan_by_unexpected_type(overall_test_results, 'expected_failures_did_not_happen', 'failure')

            if counts_unexpected['UNEXPECTED_WARNINGS'] > 0:
                print(warning('\tUnexpected warning: '))
                output_summary_test_plan_by_unexpected_type(overall_test_results, 'unexpected_warnings', 'warning')

            if counts_unexpected['EXPECTED_WARNINGS_NOT_HAPPEN'] > 0:
                print(warning('\tExpected warning did not happen: '))
                output_summary_test_plan_by_unexpected_type(overall_test_results, 'expected_warnings_did_not_happen', 'warning')

    return has_unexpected_failures


def output_summary_test_plan_by_unexpected_type(overall_test_results, key, unexpected_type):
    for test_result in overall_test_results:
        result = test_result['test_result']
        if result[key]:
            if unexpected_type == 'failure':
                print(failure('\t\t{} ({}): '.format(test_result['test_name'], test_result['log_detail_link'])))
            else:
                print(warning('\t\t{} ({}): '.format(test_result['test_name'], test_result['log_detail_link'])))
            print_failure_warning(result[key], unexpected_type, '\t\t\t')


def print_failure_warning(failure_warning_list, status, tab_format):
    for failure_warning in failure_warning_list:
        if (status == 'failure'):
            print(failure("{}Block name: '{}' - Condition: '{}'".format(tab_format, failure_warning['current_block'], failure_warning['src'])))
        else:
            print(warning("{}Block name: '{}' - Condition: '{}'".format(tab_format, failure_warning['current_block'], failure_warning['src'])))


def parser_args_cli():
    # Parser arguments list which is supplied by the user
    parser = argparse.ArgumentParser(description='Parser arguments list which is supplied by the user')

    parser.add_argument('--show-untested-test-modules', help='Flag to require show or do not show test modules which were untested', action='store_true')
    parser.add_argument('--expected-failures-file', help='Json configuration file name which records a list of expected failures/warnings', default='')
    parser.add_argument('params', nargs='+', help='List parameters contains test-plan-name and configuration-file to run all test plan. Syntax: <test-plan-name> <configuration-file> ...')

    return parser.parse_args()

if __name__ == '__main__':
    requests_session = requests.Session()

    dev_mode = False
    if 'CONFORMANCE_SERVER' in os.environ:
        api_url_base = os.environ['CONFORMANCE_SERVER']
        token_endpoint = os.environ['CONFORMANCE_TOKEN_ENDPOINT']
        client_id = os.environ['CONFORMANCE_CLIENT_ID']
        client_secret = os.environ['CONFORMANCE_CLIENT_SECRET']
        os.environ['ISSUER'] = os.environ["CONFORMANCE_SERVER"] + os.environ["TEST_CONFIG_ALIAS"]
    else:
        # local development settings
        api_url_base = 'https://localhost:8443/'
        token_endpoint = 'http://localhost:9001/token'
        client_id = 'oauth-client-1'
        client_secret = 'oauth-client-secret-1'
        dev_mode = True

        os.environ["CONFORMANCE_SERVER"] = api_url_base
        os.environ['CONFORMANCE_TOKEN_ENDPOINT'] = token_endpoint
        os.environ['CONFORMANCE_CLIENT_ID'] = client_id
        os.environ['CONFORMANCE_CLIENT_SECRET'] = client_secret
        os.environ['ISSUER'] = os.environ["CONFORMANCE_SERVER"] + os.environ["TEST_CONFIG_ALIAS"]

    if dev_mode or 'DISABLE_SSL_VERIFY' in os.environ:
        # disable https certificate validation
        requests_session.verify = False
        import urllib3

        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

    args = parser_args_cli()
    show_untested = args.show_untested_test_modules
    expected_failures_file = args.expected_failures_file
    params = args.params

    if len(params) % 2 == 1:
        print("Error: run-test-plan.py: must have even number of parameters")
        sys.exit(1)

    to_run = []
    while len(params) >= 2:
        to_run.append((params[0], params[1]))
        params = params[2:]

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

    # read json config file which records a list of expected failures/warnings
    expected_failures_json = []
    if expected_failures_file:
        with open(expected_failures_file) as f:
            expected_failures_data = f.read()
        expected_failures_json = json.loads(expected_failures_data)

    did_not_complete = False
    detail_plan_results = []
    for result in results:
        plan_result = show_plan_results(result, expected_failures_json)
        plan_did_not_complete = plan_result['plan_did_not_complete']
        if plan_did_not_complete == 'NOT_COMPLETE':
            did_not_complete = True

        detail_plan_results.append(plan_result['detail_plan_result'])

    if did_not_complete:
        print(failure("** Exiting with failure - some tests did not run completion **"))
        sys.exit(1)

    has_unexpected_failures = summary_unexpected_failures_all_test_plan(detail_plan_results)
    if has_unexpected_failures:
        print(failure("** Exiting with failure - some test modules have unexpected condition failures/warnings **"))
        sys.exit(1)

    # filter untested list, as we don't currently have test environments for these
    for m in untested_test_modules[:]:
        if all_test_modules[m]['profile'] in ['SAMPLE', 'HEART']:
            untested_test_modules.remove(m)
            continue

        if all_test_modules[m]['profile'] in ['FAPI-CIBA']:
            if re.match(r'.*-ping-.*', m):
                # ping tests are pending, see https://gitlab.com/fintechlabs/fapi-conformance-suite/issues/389
                print("Ignoring untested module: "+m)
                untested_test_modules.remove(m)
                continue

    if show_untested and len(untested_test_modules) > 0:
        print(failure("** Exiting with failure - not all available modules were tested:"))
        for m in untested_test_modules:
            print('{}: {}'.format(all_test_modules[m]['profile'], m))
        sys.exit(1)

    print(success("All tests ran to completion. See above for any test condition failures."))
    sys.exit(0)
