#!/usr/bin/env python3
#
# The script can compare the results of a particular job from two different pipelines
#
# It checks if there are any differences in the conditions run in the two jobs.
#
# It is run in our gitlab CI in the 'compare' job to compare MR against the latest master results.

import fnmatch
import json
import os
import subprocess
import sys
import tempfile
import zipfile


def load_results(artifacts_zip, is_master):
    print(artifacts_zip)
    if os.path.isdir(artifacts_zip):
        # gitlab CI gives us all the downloaded zipfiles
        owd = os.getcwd()
        os.chdir(artifacts_zip)
        results = read_zipped_results(is_master)
        os.chdir(owd)
        return results

    # otherwise user has downloaded a zip from gitlab web interface
    with tempfile.TemporaryDirectory() as tmp_dir_name:
        # print('created temporary directory', tmp_dir_name)
        os.chdir(tmp_dir_name)
        with zipfile.ZipFile(artifacts_zip, "r") as zip_ref:
            zip_ref.extractall(".")
        results = read_zipped_results(is_master)
        os.chdir("..")
    return results

def read_zipped_results(is_master):
    results = {}
    for results_zip_filename in sorted(os.listdir(".")):
        if not results_zip_filename.endswith(".zip"):
            continue
        print("Reading results from "+results_zip_filename)
        zip_name = os.path.splitext(results_zip_filename)[0]
        try:
            plan_and_variant, plan_id, _, _, _ = zip_name.rsplit("-",4)
        except:
            print("Results zip '"+zip_name+"' it not in the format <testplan>-<variants>-<planid>-dd-mmm-yyyy.zip")
            sys.exit(1)
        # print("Test: "+plan_and_variant)
        with zipfile.ZipFile(results_zip_filename, "r") as zip_ref:
            files = zip_ref.namelist()
            files = fnmatch.filter(files, "*.json")
            for f in files:
                content = zip_ref.read(f)
                test_result = json.loads(content)
                test_info = test_result['testInfo']
                # print(test_info['testName'], test_info['testId'], test_info['variant'])
                desc = test_info['description']
                if desc == None:
                    desc = "no-description"
                frozen_variant = frozenset(sorted(test_info['variant'].items()))
                if results.get(plan_and_variant + ":" + desc, {}).get(test_info['testName'], {}).get(
                    frozen_variant, None) != None:
                    # a list of existing problems that maybe we should fix sometime - don't add to this list, you
                    # can just create an extra test configuration file that has a different description so that
                    # this script can differentiate the runs
                    # If you add to the list, we may start getting random diffs in the CI compare jobs
                    ignore = [
                        "fapi-ciba-id1-brazil-discovery-end-point-verification",
                        "fapi2-security-profile-id2-discovery-end-point-verification",
                        "fapi2-security-profile-id2-client-test-happy-path",
                        "fapi2-security-profile-id2-ensure-request-object-with-multiple-aud-succeeds"
                    ]
                    if test_info['testName'] not in ignore:
                        print("More than one result for:\n{}\n{}\n{}\n".
                              format(plan_and_variant + ":" + desc, test_info['testName'], frozen_variant))
                        if not is_master:
                            # only fail if the new generated results from the new code are bad
                            sys.exit(1)

                results.setdefault(plan_and_variant + ":" + desc, {}).setdefault(test_info['testName'], {})[
                    frozen_variant] = test_result
    return results

def extract_result(log):
    """Extracts the name of each test condition run"""
    module_name = log['testInfo']["testName"]
    str=""
    for d in log['results']:
        src = d['src']
        if src == 'WebRunner' or src == 'BROWSER' or src == module_name:
            # these are asyncronous and the order isn't predictable
            continue
        str += src + "\n"
    return str

def compare(master_log, new_log):
    with tempfile.TemporaryDirectory() as tmp_dir_name:
        os.chdir(tmp_dir_name)
        with open("reference", "w") as f:
            f.write(extract_result(master_log))
        with open("new", "w") as f:
            f.write(extract_result(new_log))
        try:
            subprocess.check_output(["colordiff", "-u", "reference", "new"],
                                    stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as e:
            # diff returned non-zero, so something is different
            str = e.output.decode('utf8')
            os.chdir("..")
            return str

    os.chdir("..")
    return None

def get_url(log):
    test_info = log['testInfo']
    return log['exportedFrom']+"/log-detail.html?log="+test_info['testId']

def pretty_variant(variant_frozen):
    variant = dict(variant_frozen)
    s = ""
    for k,v in variant.items():
        s += k+":"+v+" "
    s = s[:-1]
    return s

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


def red(text):
    return color(text, bold=True, fg_red=True)

def green(text):
    return color(text, bold=True, fg_green=True)


if len(sys.argv) < 3:
    print("Syntax: "+sys.argv[0]+" <reference-artifacts-zip> <new-artifacts-zip>")
    sys.exit(1)

master_artifacts_zip = sys.argv[1]
new_artifacts_zip = sys.argv[2]

master_results = load_results(master_artifacts_zip, True)
new_results = load_results(new_artifacts_zip, False)

differences=False
for test_plan,modules in sorted(new_results.items()):
    for module,variants in sorted(modules.items()):
        for variant, log in sorted(variants.items()):
            try:
                master_log = master_results[test_plan][module][variant]
                del master_results[test_plan][module][variant]
                output = compare(master_log, log)
                if output != None:
                    differences=True
                    print("Plan: "+test_plan)
                    print("Module: "+module)
                    print("Variant: "+pretty_variant(variant))
                    print("reference log: "+get_url(master_log))
                    print("new log: "+get_url(log))
                    print("Diff output:\n"+output)
            except KeyError:
                differences=True
                print("Plan: "+test_plan)
                print("Module: "+module)
                print("Variant: "+pretty_variant(variant))
                print("new log: "+get_url(log))
                print(green("** No result for master\n"))
                next

for test_plan,modules in sorted(master_results.items()):
    for module,variants in sorted(modules.items()):
        for variant, log in sorted(variants.items()):
            differences=True
            print("Plan: "+test_plan)
            print("Module: "+module)
            print("Variant: "+pretty_variant(variant))
            print("reference log: "+get_url(log))
            print(red("** No result in new run\n"))

if differences:
    print(red("\n\nDifferences found - returning exit code 16.\n"))
    sys.exit(16) # 16 signals to the gitlab CI that the job has a warning (i.e. it didn't fail)

print(green("No differences found."))
sys.exit(0)
