import queue
import threading
import logging
from py100 import py100
from colors import *

logger = logging.getLogger('monitor')


class MonitorWorker:
    escape = chr(27)

    def __init__(self, isTTY, task_queue):
        self._isTTY = isTTY
        self._status_queue = queue.Queue()
        self._task_queue = task_queue
        self._plans = {}
        self.status_collector = threading.Thread(target=self.__run)
        self.status_collector.daemon = True
        self.status_collector.start()

    def plan_starting(self, queue_name, plan):
        self._status_queue.put_nowait((queue_name, plan, 1))

    def plan_finished(self, queue_name, plan):
        self._status_queue.put_nowait((queue_name, plan, 0))

    def test_started(self, queue_name, plan, test_id):
        self._status_queue.put_nowait((queue_name, plan, test_id))

    def test_error(self, queue_name, plan, test_id, url):
        self._status_queue.put_nowait((queue_name, plan, 5, test_id, url))

    def __run(self):
        queue_lines = 0
        while True:
            msg = self._status_queue.get()
            if msg == 'exit':
                return
            queue_name = msg[0]
            planId = msg[1]
            if self._isTTY:
                py100.move_cursor_up(self._plans.__len__() + queue_lines)
            if msg[2] == 0:
                self._plans.pop(queue_name, "")
                if self._isTTY:
                    py100.clear_entire_line()
                    print(success("queue {} : plan {} completed".format(queue_name, planId)))
                else:
                    logger.info(success("queue {} : plan {} completed".format(queue_name, planId)))
                    continue
            elif msg[2] == 5:
                if self._isTTY:
                    py100.clear_entire_line()
                    print(failure("queue {} : test {}/{} failed: check url {}".format(queue_name, msg[1], msg[3], msg[4])))
                else:
                    # logger.info("queue {} : test {}/{} failed: check url {}".format(queue_name, msg[1], msg[3], msg[4]))
                    continue
            else:
                self._plans[queue_name] = (planId, msg)

            if self._isTTY:
                queue_state = self._task_queue.dump_state_tty()
                queue_lines = len(queue_state)
                for line in queue_state:
                    py100.clear_entire_line()
                    print(color(line, bold=True, fg_orange=True))

                for a_queue_name, value in self._plans.items():
                    planid = value[0]
                    code = value[1][2]
                    py100.clear_entire_line()
                    if code == 1:
                        planType = value[1][1]
                        print("queue {} : creating plan {} ".format(a_queue_name,
                                                                    (planType[:75] + '..') if len(
                                                                        planType) > 75 else planType))
                    else:
                        print("queue {} : plan {} running test {}".format(a_queue_name, value[1][1], value[1][2]))
            elif msg[2] == 1:
                planType = msg[1]
                # logger.info("queue {} : creating plan {} ".format(queue_name,
                #                                                   (planType[:75] + '..') if len(planType) > 75 else planType))
            # else:
            #     logger.info("queue {} : plan {} running test {}".format(queue_name, msg[1], msg[2]))
