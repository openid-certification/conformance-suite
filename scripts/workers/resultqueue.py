import threading
import queue
import fnmatch
import logging
import time

from workers.taskqueue import TestTask

logger = logging.getLogger('resultqueue')

class ResultQueue:

    def __init__(self, expected_failures_list):
        self._all_results = {}
        self.slowest_test_plan = []
        self.slowest_test_module = []
        self._expected_failures_list = expected_failures_list
        # self.expected_skip_list = expected_skip_list
        self._tests_to_retry = []
        self._lock = threading.Lock()

        # created an unbounded queue as the rate of incoming requests is lower than expected summarization proc
        self._queue = queue.Queue()
        consumer = threading.Thread(target=self.consumer)
        consumer.daemon = True
        consumer.start()

    def add_task_result(self, task: TestTask, result):
        self._queue.put((task, result))

    def get_task_results(self):
        self._queue.join()
        self._all_results["tests_to_retry"] = self._tests_to_retry

        return self._all_results

    # consume work
    def consumer(self):
        print('Consumer: Running')
        # consume work
        while True:
            # get a unit of work
            key = self._queue.get()
            # check for stop
            if key is None:
                self._queue.task_done()
                break

            task = key[0]
            result = key[1]
            succeded = {}
            expected_failed = {}
            failed = {}
            src = task.config["src"]
            all_passed = True
            failed_tests_links = []
            for test, results in result.items():
                if test == 'variant' or test == "url":
                    continue
                test_info = results['info'] if 'info' in results else {}
                test_result = test_info['result'] if 'result' in test_info else "FAILED"
                all_passed = all_passed and test_result == "PASSED"
                if test_result == "PASSED" or test_result == "WARNING" or test_result == "REVIEW":
                    succeded[test] = test_result
                else:
                    if self.expected_failure(self._expected_failures_list, test_info['testName'],  test_info["variant"], task.plan_config_filename, results):
                        expected_failed[test] = test_result
                    else:
                        failed[test] = test_result
                        failed_tests_links.append(results['url'])
                if 'op' in results:
                    op_tests = results['op']
                    op_variant = op_tests['variant']
                    op_config = op_tests['config']
                    op_modules = op_tests['tests']
                    for op_test_name, op_module in op_modules.items():

                        op_test_info = op_module['info'] if 'info' in op_module else {}
                        op_test_result = op_test_info['result'] if 'result' in op_test_info else "FAILED"
                        if op_test_result == "PASSED" or op_test_result == "REVIEW":
                            succeded[op_test_name] = op_test_info
                        else:
                            if self.expected_failure(self._expected_failures_list, op_test_info['testName'],  op_variant, op_config, op_module):
                                expected_failed[op_test_name] = op_test_info
                            else:
                                failed[op_test_name] = op_test_info
                                failed_tests_links.append(f"{results['url']} - failed OP: {op_module['url']} ")

            if failed:
                self._tests_to_retry.append(task.config["src"])

            self._all_results[src] = {
                "succeeded": len(succeded),
                "failed": len(failed),
                "expected_failed": len(expected_failed),
                "failed_tests_links": failed_tests_links,
            }
            self._queue.task_done()
        # all done
        print('Consumer: Done')

    def expected_failure(self, expected_failures, testname, variant, config_filename, test_result):

        # much simpler using a closure than currying
        def is_expected_for_this_test(obj):
            if obj['test-name'] != testname or not fnmatch.fnmatch(config_filename, obj['configuration-filename']):
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

        test_expected_failures = list(filter(is_expected_for_this_test, expected_failures))

        # Filter the starting of blocks and entries with a result
        block_names = {}
        block_msg = ''
        for log_entry in test_result["logs"]:
            if ('startBlock' in log_entry and log_entry['startBlock'] == True and log_entry['src'] == '-START-BLOCK-'):
                block_names[log_entry['blockId']] = log_entry['msg']
                continue
            if 'result' not in log_entry:
                continue
            if log_entry["result"] == "FAILURE":
                is_expected = False
                for expected_failure in test_expected_failures:
                    if expected_failure["current-block"] != "" and expected_failure["current-block"] != log_entry["blockId"]:
                        continue
                    if expected_failure["condition"] == log_entry["src"]:
                        is_expected = True
                if not is_expected:
                    return False
        return True
