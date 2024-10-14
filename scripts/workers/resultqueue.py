import threading

from workers.taskqueue import TestTask


class ResultQueue:
    def __init__(self):
        self._all_results= {}
        self._lock = threading.Lock()

    def add_task_result(self, task: TestTask, result):
        with self._lock:
            self._all_results[(task.config, task.plan_config_filename)] = result

    def get_task_results(self):
        with self._lock:
            return self._all_results
