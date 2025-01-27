import asyncio
import collections
import logging
import queue
import threading
import time
from dataclasses import dataclass, field
from typing import Any


@dataclass(order=True)
class PrioritizedItem:
    priority: int
    item: Any = field(compare=False)


logger = logging.getLogger(__name__)


class TestTask:
    def __init__(self, alias: str, config: str, plan_config_filename: str, config_json, parsed_config):
        self.alias = alias
        self.config = config
        self.plan_config_filename = plan_config_filename
        self.config_json = config_json
        self.parsed_config = parsed_config

    @property
    def queue_name(self) -> str:
        return self.alias


class TaskQueue:
    def __init__(self):
        self._none_queue = None
        self._all_queues = {}
        self._running_queues = {}
        self._priority_queue = queue.PriorityQueue()
        self._pending_executions = False
        self._running_jobs = 0
        self._lock = threading.Lock()

    def add_task(self, task: TestTask):
        if task.queue_name not in self._all_queues:
            self._all_queues[task.queue_name] = collections.deque()
        self._all_queues[task.queue_name].append(task)

    def fill_priority_queue(self):
        for alias, aQueue in self._all_queues.items():
            self._priority_queue.put(PrioritizedItem(-1 * len(aQueue), aQueue))
            if alias is None:
                self._none_queue = aQueue

    def get_a_task(self) -> TestTask:
        logger.debug("pulling items from priority queue")
        aQueue = self._priority_queue.get().item
        task = aQueue.popleft()
        logger.debug(f"priority - picked from queue {task.queue_name}")
        if task.queue_name is not None and len(aQueue) > 0:
            self._running_queues[task.queue_name] = aQueue
        with self._lock:
            self._pending_executions += 1
        if task.queue_name is None and len(aQueue) > 0:
            self._priority_queue.put(PrioritizedItem(-1 * len(aQueue), aQueue))
        return task

    def task_completed(self, task: TestTask):
        queue_name = task.queue_name
        logger.debug(f"queue {queue_name} - starting the completion")
        if queue_name in self._running_queues:
            aQueue = self._running_queues.pop(queue_name)
            logger.info(f"queue {queue_name} - tasks to go {len(aQueue)}")
            self._priority_queue.put(PrioritizedItem(-1 * len(aQueue), aQueue))
            logger.debug(f"queue {queue_name} - queue included on priority queue")
        elif queue_name is None:
            if len(self._none_queue) > 0:
                logger.info(f"queue {queue_name} - tasks to go {len(self._none_queue)}")
            else:
                logger.info(f"queue {queue_name} - queue cleared. it is done")
        else:
            logger.info(f"queue {queue_name} - queue cleared. it is done")
        with self._lock:
            self._pending_executions -= 1

    def dump_state_logger(self):
        for alias, aQueue in self._all_queues.items():
            if len(aQueue) > 0:
                logger.info(f"Queue {alias} has {len(aQueue)} elements")

    def dump_state_tty(self):
        state = []
        for queue_name, aQueue in self._all_queues.items():
            if len(aQueue) > 0:
                state.append(f"Queue {queue_name} has {len(aQueue)} elements")
        state.append("Running {} tasks".format(self._pending_executions))
        return state

    def join(self, with_dump_state=True):
        all_done = False
        while not all_done:
            time.sleep(5)
            all_done = True
            for alias, aQueue in self._all_queues.items():
                all_done = all_done and len(aQueue) == 0
            if all_done:
                with self._lock:
                    all_done = self._pending_executions <= 0

            if with_dump_state:
                self.dump_state_logger()
