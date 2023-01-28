#!/usr/bin/env python3
#
# Checks for incorrect whitespace in source files

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import subprocess
import sys
import argparse

from whitespace import Whitespace

source_directory = os.path.join(sys.path[0], '..')

extensions_to_check = [
    'java',
    'py',
    'js',
    'json',
    'html',
    'css',
    'txt',
    'gitignore',
    'yml',
    'md',
    'xml',
    'yaml',
    'conf',
    'sh'
]

config_ignore = [
    '.idea/'
]

extensions_no_tabs = [
    'html',
    'py',
    'yml',
    'sh'
]


def ignore_filename(filename):
    for pattern in config_ignore:
        if filename.startswith(pattern):
            return True
    return False


parser = argparse.ArgumentParser()
parser.add_argument("--fix", help="automatically fix errors", action="store_true")
parser.add_argument("--verbose", help="show verbose output", action="store_true")
args = parser.parse_args()

# only check files known to git - this neatly avoids us processing git ignored files, generated files, etc
command = ['git', 'ls-files']
files = subprocess.check_output(command).decode('utf8')
fixed = False
errors = False
for f in files.split('\n'):
    if len(f) == 0:
        continue
    path, extension = os.path.splitext(f)
    extension = extension[1:]

    if extension not in extensions_to_check:
        if args.verbose:
            print("Skipping '{}': not in extensions_to_check".format(f))
        continue

    if ignore_filename(f):
        if args.verbose:
            print("Skipping '{}': listed in config_ignore".format(f))
        continue

    if args.verbose:
        print("Checking "+f)

    with open(f) as fh:
        lines = fh.read()

    # print("File " + f + " contents:\n" + lines)
    whitespace = Whitespace(lines, fix=args.fix)
    if whitespace.dos_line_endings():
        print('{} has DOS line endings'.format(f))
        errors = True

    if whitespace.has_trailing():
        print('{} contains trailing whitespace'.format(f))
        errors = True

    if whitespace.missing_final_newline():
        print('{} is missing final newline'.format(f))
        errors = True

    if whitespace.blank_lines_at_end():
        print('{} has blank lines at end'.format(f))
        errors = True

    if extension in extensions_no_tabs and whitespace.has_tabs():
        print('{} has tabs'.format(f))
        errors = True

    if whitespace.fixed:
        fixed = True
        with open(f, 'w') as fh:
            fh.write(whitespace.lines)
        print('{} has been fixed'.format(f))

if fixed:
    print('All errors fixed')

if errors:
    sys.exit(1)
