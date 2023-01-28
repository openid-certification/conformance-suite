#!/usr/bin/env python3
#
# Python module for dealing with whitespace etc in source files

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import unittest

import re


class Whitespace(object):
    def __init__(self, lines, fix=False):
        self.lines = lines
        self.fix = fix
        self.fixed = False

    def has_trailing(self):
        m = re.search(r'[ \t]+\n', self.lines)
        if m:
            if self.fix:
                self.fixed = True
                self.lines = re.sub(r'[ \t]+\n', '\n', self.lines)
            return True
        return False

    def missing_final_newline(self):
        m = not self.lines.endswith('\n')
        if m:
            if self.fix:
                self.fixed = True
                self.lines += '\n'
            return True
        return False

    def blank_lines_at_end(self):
        if self.lines.endswith('\n\n'):
            if self.fix:
                self.fixed = True
                self.lines = self.lines.rstrip() + '\n'
            return True
        return False

    def dos_line_endings(self):
        if '\r' in self.lines:
            if self.fix:
                self.fixed = True
                self.lines = self.lines.replace('\r', '')
            return True
        return False

    def has_tabs(self):
        if '\t' in self.lines:
            if self.fix:
                print("** Tab errors cannot be fixed automatically")
            return True
        return False


class Test(unittest.TestCase):
    test_good = "foo\nbar\n\n    cheese\n\tcheese\n"

    def test_trail(self):
        test_trailing = "foo\n\n    bar   \n\tcheese\t\n"
        test_trailing_fixed = "foo\n\n    bar\n\tcheese\n"
        whitespace = Whitespace(test_trailing, fix=False)
        self.assertEqual(whitespace.has_trailing(), True)

        whitespace = Whitespace(self.test_good, fix=False)
        self.assertEqual(whitespace.has_trailing(), False)

        whitespace = Whitespace(test_trailing, fix=True)
        self.assertEqual(whitespace.has_trailing(), True)
        self.assertEqual(whitespace.lines, test_trailing_fixed)

        whitespace = Whitespace(self.test_good, fix=True)
        self.assertEqual(whitespace.has_trailing(), False)
        self.assertEqual(whitespace.lines, self.test_good)

    def test_final_newline(self):
        test_missing_newline = "foo\nbar\n\n    cheese\n\tcheese"
        test_missing_newline_fixed = test_missing_newline + "\n"

        whitespace = Whitespace(test_missing_newline, fix=False)
        self.assertEqual(whitespace.missing_final_newline(), True)

        whitespace = Whitespace(self.test_good, fix=False)
        self.assertEqual(whitespace.missing_final_newline(), False)

        whitespace = Whitespace(test_missing_newline, fix=True)
        self.assertEqual(whitespace.missing_final_newline(), True)
        self.assertEqual(whitespace.lines, test_missing_newline_fixed)

        whitespace = Whitespace(self.test_good, fix=True)
        self.assertEqual(whitespace.missing_final_newline(), False)
        self.assertEqual(whitespace.lines, self.test_good)

    def test_blank_lines(self):
        test_blank_lines_fixed = "foo\nbar\n\n    cheese\n\tcheese\n"
        test_blank_lines = test_blank_lines_fixed + "\n\n\n"

        whitespace = Whitespace(test_blank_lines, fix=False)
        self.assertEqual(whitespace.blank_lines_at_end(), True)

        whitespace = Whitespace(self.test_good, fix=False)
        self.assertEqual(whitespace.blank_lines_at_end(), False)

        whitespace = Whitespace(test_blank_lines, fix=True)
        self.assertEqual(whitespace.blank_lines_at_end(), True)
        self.assertEqual(whitespace.lines, test_blank_lines_fixed)

        whitespace = Whitespace(self.test_good, fix=True)
        self.assertEqual(whitespace.blank_lines_at_end(), False)
        self.assertEqual(whitespace.lines, self.test_good)

    def test_dos(self):
        test_dos = "foo\r\nbar\n\n    cheese\r\n\tcheese\n"
        test_dos_fixed = "foo\nbar\n\n    cheese\n\tcheese\n"

        whitespace = Whitespace(test_dos, fix=False)
        self.assertEqual(whitespace.dos_line_endings(), True)

        whitespace = Whitespace(self.test_good, fix=False)
        self.assertEqual(whitespace.dos_line_endings(), False)

        whitespace = Whitespace(test_dos, fix=True)
        self.assertEqual(whitespace.dos_line_endings(), True)
        self.assertEqual(whitespace.lines, test_dos_fixed)

        whitespace = Whitespace(self.test_good, fix=True)
        self.assertEqual(whitespace.dos_line_endings(), False)
        self.assertEqual(whitespace.lines, self.test_good)

    def test_tab(self):
        test_good_no_tabs = "foo\nbar\n\n    cheese\ncheese\n"
        test_tab = "foo\n\tbar\n\n    \tcheese\n\tcheese\n"
        # can't fix tabs as we don't know what tab stop width dev is using
        test_tab_fixed = test_tab

        whitespace = Whitespace(test_tab, fix=False)
        self.assertEqual(whitespace.has_tabs(), True)

        whitespace = Whitespace(test_good_no_tabs, fix=False)
        self.assertEqual(whitespace.has_tabs(), False)

        whitespace = Whitespace(test_tab, fix=True)
        self.assertEqual(whitespace.has_tabs(), True)
        self.assertEqual(whitespace.lines, test_tab_fixed)

        whitespace = Whitespace(test_good_no_tabs, fix=True)
        self.assertEqual(whitespace.has_tabs(), False)
        self.assertEqual(whitespace.lines, test_good_no_tabs)


if __name__ == '__main__':
    unittest.main()
