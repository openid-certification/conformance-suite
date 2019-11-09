#!/usr/bin/env python3

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

def split_name_and_variant(test_plan):
    if '[' in test_plan:
        name = re.match(r'^[^\[]*', test_plan).group(0)
        vs = re.finditer(r'\[([^=\]]*)=([^\]]*)\]', test_plan)
        variant = { v.group(1) : v.group(2) for v in vs }
        return (name, variant)
    else:
        return (test_plan, None)

def run_test_plan(test_plan, config_file):
    print("Running plan '{}' with configuration file '{}'".format(test_plan, config_file))
    with open(config_file) as f:
        json_config = f.read()
    (test_plan_name, variant) = split_name_and_variant(test_plan)
    test_plan_info = conformance.create_test_plan(test_plan_name, json_config, variant)
    plan_id = test_plan_info['id']
    plan_modules = test_plan_info['modules']
    test_info = {}  # key is module name
    test_time_taken = {}  # key is module_id
    overall_start_time = time.time()
    print('Created test plan, new id: {}'.format(plan_id))
    print('{}plan-detail.html?plan={}'.format(api_url_base, plan_id))
    print('{:d} modules to test:\n{}\n'.format(len(plan_modules), '\n'.join(plan_modules)))
    for module in plan_modules:
        test_start_time = time.time()
        module_id = ''
        module_info = {}

        try:
            print('Running test module: {}'.format(module))
            test_module_info = conformance.create_test_from_plan(plan_id, module)
            module_id = test_module_info['id']
            module_info['id'] = module_id
            test_info[module] = module_info
            print('Created test module, new id: {}'.format(module_id))
            print('{}log-detail.html?log={}'.format(api_url_base, module_id))

            state = conformance.wait_for_state(module_id, ["WAITING", "FINISHED"])

            if state == "WAITING":
                # TODO branch here for OIDCC tests
                # TODO set os.environ['VARIANT'] e.g. '{"client_auth_type":"client_secret_basic","request_type":"plain_http_request","response_type":"code","client_registration":"dynamic_client"}'
                # TODO set os.environ['MODULE_NAME'] e.g. 'oidcc-client-test-client-secret-basic'
                # TODO set os.environ['ISSUER'] to the issuer identifier value
                # TODO: subprocess.call(["npm", "run", "client"], cwd="./sample-openid-client-nodejs")
                if re.match(r'(oidcc-client-.*)', module):
                    print('Running OIDCC Client tests')

                    oidcc_variant_str = json.dumps(variant)
                    oidcc_issuer_str = os.environ["CONFORMANCE_SERVER"] + os.environ["OIDCC_TEST_CONFIG_ALIAS"]

                    print('VARIANT {}'.format(oidcc_variant_str))
                    print('MODULE_NAME {}'.format(module))
                    print('ISSUER {}'.format(oidcc_issuer_str))

                    os.putenv('VARIANT', oidcc_variant_str)
                    os.putenv('MODULE_NAME', module)
                    os.putenv('ISSUER', oidcc_issuer_str)
                    subprocess.call(["npm", "run", "client"], cwd="./sample-openid-client-nodejs")
                # If it's a client test, we need to run the client
                elif re.match(r'(fapi-rw-id2(-ob)?-client-.*)', module):
                    profile = variant['fapi_profile']
                    os.putenv('CLIENTTESTMODE', 'fapi-ob' if re.match(r'openbanking', profile) else 'fapi-rw')
                    os.environ['ISSUER'] = os.environ["CONFORMANCE_SERVER"] + os.environ["TEST_CONFIG_ALIAS"]
                    subprocess.call(["npm", "run", "client"], cwd="./sample-openbanking-client-nodejs")

                conformance.wait_for_state(module_id, ["FINISHED"])

        except Exception as e:
            print('Exception: Test {} failed to run to completion: {}'.format(module, e))
        if module_id != '':
            test_time_taken[module_id] = time.time() - test_start_time
            module_info['info'] = conformance.get_module_info(module_id)
            module_info['logs'] = conformance.get_test_log(module_id)
    overall_time = time.time() - overall_start_time
    print('\n\n')
    return {
        'test_plan': test_plan,
        'config_file': config_file,
        'plan_id': plan_id,
        'plan_modules': plan_modules,
        'test_info': test_info,
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

    print('\n\nResults for {} with configuration {}:'.format(plan_result['test_plan'], plan_result['config_file']))

    for module in plan_modules:
        if module not in test_info:
            print(failure('Test {} did not run'.format(module)))
            continue
        module_info = test_info[module]
        module_id = module_info['id']
        info = module_info['info']
        logs = module_info['logs']

        status_coloured = info['status']

        if info['status'] != 'FINISHED':
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
              format(module, module_id, status_coloured, result_coloured, len(logs),
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

    for module in plan_modules:
        if module not in test_info:
            continue
        module_info = test_info[module]
        module_id = module_info['id']
        info = module_info['info']
        logs = module_info['logs']

        if module in untested_test_modules:
            untested_test_modules.remove(module)

        if info['status'] != 'FINISHED':
            incomplete += 1
        if 'result' not in info:
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

    if (len(test_info) != len(plan_modules) or incomplete != 0):
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
    (test_plan_name, variant) = split_name_and_variant(test_plan)

    def is_expected_for_this_test(obj):
        return (obj['test-name'] == test_name
                and obj['configuration-filename'] == config_filename
                and obj.get('variant', None) == variant)

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
                    test_expected_failures.remove(expected_failure_obj)
                    expected_failures_list.remove(expected_failure_obj)
                    break

            # list all the unexpected failures/warnings of a test module
            if log_entry_exist_in_expected_list == False:
                if log_result == 'FAILURE':
                    unexpected_failures.append({'current_block': block_msg, 'src': log_entry['src']})
                    counts_unexpected['UNEXPECTED_FAILURES'] += 1

                if log_result == 'WARNING':
                    unexpected_warnings.append({'current_block': block_msg, 'src': log_entry['src']})
                    counts_unexpected['UNEXPECTED_WARNINGS'] += 1

    # list all the expected failures/warnings did not happen of a test module
    for expected_failure_obj in test_expected_failures:
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
        if test_result == 'SKIPPED':
            expected_skip = True
            expected_skips_list.remove(expected_skip_obj)
        else:
            expected_skip_did_not_happen = True
            counts_unexpected['EXPECTED_SKIPS_NOT_HAPPEN'] += 1

    if (test_result == 'SKIPPED' and not expected_skip):
        unexpected_skip = True
        counts_unexpected['UNEXPECTED_SKIPS'] += 1

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
            (test_plan_name, variant) = split_name_and_variant(test_plan)

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


def output_summary_test_plan_by_unexpected_type(variant, config_filename, overall_test_results, key, unexpected_type):
    for test_result in overall_test_results:
        result = test_result['test_result']
        if result[key]:
            test_name = test_result['test_name']
            if unexpected_type == 'failure':
                print(failure('\t\t{} ({}): '.format(test_name, test_result['log_detail_link'])))
            else:
                print(warning('\t\t{} ({}): '.format(test_name, test_result['log_detail_link'])))
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
                    print("Template expected failure json:\n")
                    # print json, skipping timestamp addition for easy C&P
                    print(json.dumps(template, indent=4)+",\n", file=sys.__stdout__)
        else:
            if expected:
                print(expected_warning(msg))
            else:
                print(warning(msg))
                if print_template:
                    print("Template expected warning json:\n")
                    # print json, skipping timestamp addition for easy C&P
                    print(json.dumps(template, indent=4)+",\n", file=sys.__stdout__)


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

    parser.add_argument('--show-untested-test-modules', help='Flag to require show or do not show test modules which were untested', default='')
    parser.add_argument('--expected-failures-file', help='Json configuration file name which records a list of expected failures/warnings', default='')
    parser.add_argument('--expected-skips-file', help='Json configuration file name which records a list of expected skipped tests', default='')
    parser.add_argument('params', nargs='+', help='List parameters contains test-plan-name and configuration-file to run all test plan. Syntax: <test-plan-name> <configuration-file> ...')

    return parser.parse_args()

if __name__ == '__main__':
    requests_session = requests.Session()

    dev_mode = False
    if 'CONFORMANCE_SERVER' in os.environ:
        api_url_base = os.environ['CONFORMANCE_SERVER']
        token = os.environ['CONFORMANCE_TOKEN']
    else:
        # local development settings
        api_url_base = 'https://localhost:8443/'
        token = None
        dev_mode = True

        os.environ["CONFORMANCE_SERVER"] = api_url_base

    if dev_mode or 'DISABLE_SSL_VERIFY' in os.environ:
        # disable https certificate validation
        requests_session.verify = False
        import urllib3

        urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

    args = parser_args_cli()
    show_untested = args.show_untested_test_modules
    params = args.params

    if len(params) % 2 == 1:
        print("Error: run-test-plan.py: must have even number of parameters")
        sys.exit(1)

    to_run = []
    while len(params) >= 2:
        to_run.append((params[0], params[1]))
        params = params[2:]

    conformance = Conformance(api_url_base, token, requests_session)

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

    expected_failures_list = []
    if args.expected_failures_file:
        expected_failures_list = load_expected_problems(args.expected_failures_file)

    expected_skips_list = []
    if args.expected_skips_file:
        expected_skips_list = load_expected_problems(args.expected_skips_file)

    results = []
    for (plan_name, config_json) in to_run:
        result = run_test_plan(plan_name, config_json)
        results.append(result)

    print("\n\nScript complete - results:")

    did_not_complete = False
    failed_plan_results = []
    for result in results:
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

    # analyze_plan_results will remove expected failures and skips from the list, so if
    # any remain at this point, they are unused and should be warned about.
    if expected_failures_list:
        print(warning("** Some expected failures were not found in any test module of the system **"))
        for entry in expected_failures_list:
            entry_invalid_json = {
                'test-name': entry['test-name'],
                'variant': entry.get('variant', None),
                'configuration-filename': entry['configuration-filename'],
                'current-block': entry['current-block'],
                'condition': entry['condition'],
                'expected-result': entry['expected-result']
            }
            print(json.dumps(entry_invalid_json, indent=4) + "\n", file=sys.__stdout__)

    if expected_skips_list:
        print(warning("** Some expected skips were not found in any test module of the system **"))
        for entry in expected_skips_list:
            entry_invalid_json = {
                'test-name': entry['test-name'],
                'variant': entry.get('variant', None),
                'configuration-filename': entry['configuration-filename']
            }
            print(json.dumps(entry_invalid_json, indent=4) + "\n", file=sys.__stdout__)

    if failed_plan_results:
        summary_unexpected_failures_all_test_plan(failed_plan_results)
        print(failure("** Exiting with failure - some test modules have unexpected condition failures/warnings **"))
        sys.exit(1)

    # filter untested list, as we don't currently have test environments for these
    for m in untested_test_modules[:]:
        if all_test_modules[m]['profile'] in ['HEART']:
            untested_test_modules.remove(m)
            continue

        if show_untested == 'client':
            # Only run client test, therefore ignore all server test
            if not ( re.match(r'(fapi-rw-id2-client-.*)', m) or re.match(r'(fapi-rw-id2-ob-client-.*)', m)  or re.match(r'(oidcc-client-.*)', m) ):
                untested_test_modules.remove(m)
                continue
        elif show_untested == 'server':
            # Only run server test, therefore ignore all client test
            if re.match(r'(fapi-rw-id2-client-.*)', m) or re.match(r'(fapi-rw-id2-ob-client-.*)', m) or re.match(r'(fapi-ciba-id1.*)', m):
                untested_test_modules.remove(m)
                continue
        elif show_untested == 'ciba':
            # Only run server test, therefore ignore all ciba test
            if not re.match(r'(fapi-ciba-id1.*)', m):
                untested_test_modules.remove(m)
                continue

    if show_untested and len(untested_test_modules) > 0:
        print(failure("** Exiting with failure - not all available modules were tested:"))
        for m in untested_test_modules:
            print('{}: {}'.format(all_test_modules[m]['profile'], m))
        sys.exit(1)

    print(success("All tests ran to completion. See above for any test condition failures."))
    sys.exit(0)
