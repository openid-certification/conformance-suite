import threading
import time
import unittest

from workers.taskqueue import TaskQueue


class MyTestCase(unittest.TestCase):

    def setUp(self):
        self.queue = TaskQueue()
        self.queue.add_task("alias1","plan0",None,None)
        self.queue.add_task("alias1","plan1",None,None)
        self.queue.add_task("alias1","plan2",None,None)
        self.queue.add_task("alias1","plan3",None,None)
        self.queue.add_task("alias2","plan4",None,None)
        self.queue.add_task("alias2","plan5",None,None)
        self.queue.add_task("alias2","plan6",None,None)
        self.queue.add_task(None,"plan7",None,None)
        self.queue.add_task(None,"plan8",None,None)
        self.queue.fill_priority_queue()

    def test_running_queue(self):
        task = self.queue.get_a_task()
        self.assertEqual("alias1", task[0])
        self.assertEqual("plan0", task[1])
        anotherTask = self.queue.get_a_task()
        self.assertFalse(anotherTask[0] == task[0])
        self.queue.task_completed(task)
        task = self.queue.get_a_task()
        self.assertEqual("alias1", task[0])
        self.assertEqual("plan1", task[1])

    def test_join_queue(self):
        processed = 0
        def take_elements():
            nonlocal processed
            while True:
                task = self.queue.get_a_task()
                time.sleep(0.1)
                processed = processed + 1
                self.queue.task_completed(task)

        t = threading.Thread(target=take_elements)
        t.daemon = True
        t.start()
        self.queue.join()
        self.assertEqual(processed, 9)


if __name__ == '__main__':
    unittest.main()
