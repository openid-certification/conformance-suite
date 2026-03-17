#!/usr/bin/env python3
#
# compare-results.py — Compare conformance suite CI results across pipelines
#
# Used in the GitLab CI 'compare' job to detect regressions: it compares the
# test conditions executed in each test module between a reference run (master)
# and a new run (MR branch). Any difference in the ordered list of conditions
# is reported as a diff.
#
# Usage:
#   compare-results.py <reference-artifacts-zip> <new-artifacts-zip>
#
# Each argument is either a directory of per-plan zip files (as downloaded by
# GitLab CI) or a single zip downloaded from the GitLab web interface.
#
# Exit codes:
#   0  — no differences found
#   16 — differences found (GitLab CI treats this as a warning, not a failure)
#   1  — fatal error (bad input, duplicate results, etc.)
#
# Matching logic
# ==============
#
# Results are keyed by (plan key, module name, variant set). When branches
# rename variant keys/values or add new variant parameters, exact matching
# breaks. Two levels of fuzzy matching handle this:
#
# Plan-level fuzzy matching (find_fuzzy_plan_match)
# -------------------------------------------------
# Plan keys have the format "plan-name-variant1-variant2-...:description".
# When variant values change (e.g. "haip" → "vci_haip"), the plan key
# changes even though it is logically the same plan. The fuzzy matcher:
#   1. Requires the description suffix (after the last ":") to match exactly.
#   2. Tokenises the plan-name prefix on "-" and computes Jaccard similarity
#      (|intersection| / |union|) between the token sets.
#   3. Picks the best match with Jaccard >= 0.5.
#
# Variant-level fuzzy matching (find_fuzzy_variant_match)
# -------------------------------------------------------
# Variants are frozensets of (key, value) pairs. Two strategies are tried
# in order:
#
#   Strategy 1 — Subset/superset
#     Matches when one variant set is a subset of the other. This handles
#     the common case where a new variant parameter is added (the new
#     variant is a superset of the old one) or removed.
#
#   Strategy 2 — Key-value overlap
#     Handles key renames (e.g. vci_profile:haip → fapi_profile:vci_haip).
#     Counts exact (key, value) pair matches between the two variants.
#     Requires:
#       - at least 2 exact pair matches
#       - at most 2 unmatched pairs in the smaller variant set
#     Picks the candidate with the most exact matches.
#
# Module-level fuzzy matching (find_fuzzy_module_match)
# -----------------------------------------------------
# Module names are hyphen-separated identifiers. When a module is renamed
# (e.g. "ensure-authorization-request-with-long-nonce" becomes
# "ensure-request-object-with-long-nonce"), exact lookup fails. The fuzzy
# matcher tokenises both names on "-" and picks the best Jaccard match
# with similarity >= 0.5.
#
# When a fuzzy match is used, the output includes an annotation showing the
# reference plan key, module name, or variant that was matched against.

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

def pretty_variant_diff(new_variant, ref_variant):
    new_dict = dict(new_variant)
    ref_dict = dict(ref_variant)
    all_keys = sorted(set(new_dict) | set(ref_dict))
    lines = []
    for k in all_keys:
        in_new = k in new_dict
        in_ref = k in ref_dict
        if in_new and in_ref and new_dict[k] == ref_dict[k]:
            continue
        elif in_new and in_ref:
            lines.append(red("- %s:%s" % (k, ref_dict[k])))
            lines.append(green("+ %s:%s" % (k, new_dict[k])))
        elif in_new:
            lines.append(green("+ %s:%s" % (k, new_dict[k])))
        else:
            lines.append(red("- %s:%s" % (k, ref_dict[k])))
    return "\n".join(lines)

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

def yellow(text):
    return color(text, bold=True, fg_yellow=True)


if len(sys.argv) < 3:
    print("Syntax: "+sys.argv[0]+" <reference-artifacts-zip> <new-artifacts-zip>")
    sys.exit(1)

master_artifacts_zip = sys.argv[1]
new_artifacts_zip = sys.argv[2]

master_results = load_results(master_artifacts_zip, True)
new_results = load_results(new_artifacts_zip, False)

def find_fuzzy_variant_match(master_module_variants, new_variant):
    """Find a master variant that fuzzy-matches the new variant.

    Tries two strategies in order:
    1. Subset/superset — handles added/removed variant keys
    2. Key-value overlap — handles key renames (e.g., vci_profile:haip →
       fapi_profile:vci_haip) by requiring that the majority of key-value
       pairs match exactly and at most 2 pairs differ on each side.

    Returns the matching master variant frozenset, or None.
    """
    # Strategy 1: subset/superset
    for master_variant in master_module_variants:
        if master_variant <= new_variant or new_variant <= master_variant:
            return master_variant

    # Strategy 2: key-value overlap with tolerance for renamed keys and
    # added variant parameters.  Require that almost all of the smaller
    # variant's keys matched (at most 2 unmatched — covering key renames),
    # while allowing the larger variant to have any number of extra keys
    # (covering added variant parameters across branches).
    new_dict = dict(new_variant)
    best_match = None
    best_matched_count = 0
    for master_variant in master_module_variants:
        master_dict = dict(master_variant)
        # Count key-value pairs that match exactly (same key, same value)
        matched = sum(1 for k, v in new_dict.items()
                      if master_dict.get(k) == v)
        new_only = len(new_dict) - matched
        master_only = len(master_dict) - matched
        smaller_unmatched = min(new_only, master_only)
        if (matched > best_matched_count
                and smaller_unmatched <= 2
                and matched >= 2):
            best_matched_count = matched
            best_match = master_variant

    return best_match

def find_fuzzy_plan_match(master_results, new_test_plan):
    """Find a master plan key that fuzzy-matches the new plan key.

    Plan keys have the format 'plan-name-variant1-variant2-...:description'.
    When variant values change (e.g., 'haip' becomes 'vci_haip') or variant
    parameters are added/removed, the plan key changes even though it's
    logically the same plan.

    Match by: same description, and the plan name tokens (split by '-') have
    high overlap (Jaccard similarity >= 0.5).

    Returns the matching master plan key, or None.
    """
    if ":" not in new_test_plan:
        return None
    new_plan_part, new_desc = new_test_plan.rsplit(":", 1)
    new_tokens = set(new_plan_part.split("-"))

    best_match = None
    best_score = 0.0
    for master_plan in master_results:
        if ":" not in master_plan:
            continue
        master_plan_part, master_desc = master_plan.rsplit(":", 1)
        if master_desc != new_desc:
            continue
        master_tokens = set(master_plan_part.split("-"))
        intersection = len(new_tokens & master_tokens)
        union = len(new_tokens | master_tokens)
        if union == 0:
            continue
        score = intersection / union
        if score > best_score and score >= 0.5:
            best_score = score
            best_match = master_plan
    return best_match

def find_fuzzy_module_match(master_modules, new_module):
    """Find a master module name that fuzzy-matches the new module name.

    Module names are hyphen-separated identifiers. When a module is renamed
    (e.g. 'ensure-authorization-request-with-long-nonce' becomes
    'ensure-request-object-with-long-nonce'), exact lookup fails. This
    matcher tokenises both names on '-' and picks the best Jaccard match
    with similarity >= 0.5.

    Returns the matching master module name, or None.
    """
    new_tokens = set(new_module.split("-"))
    best_match = None
    best_score = 0.0
    for master_module in master_modules:
        master_tokens = set(master_module.split("-"))
        intersection = len(new_tokens & master_tokens)
        union = len(new_tokens | master_tokens)
        if union == 0:
            continue
        score = intersection / union
        if score > best_score and score >= 0.5:
            best_score = score
            best_match = master_module
    return best_match

differences=False
for test_plan,modules in sorted(new_results.items()):
    # If this plan key doesn't exist in master, try fuzzy plan matching
    master_plan_key = test_plan
    fuzzy_plan = False
    if test_plan not in master_results:
        matched_plan = find_fuzzy_plan_match(master_results, test_plan)
        if matched_plan is not None:
            master_plan_key = matched_plan
            fuzzy_plan = True

    for module,variants in sorted(modules.items()):
        for variant, log in sorted(variants.items()):
            master_log = None
            matched_variant = None
            matched_module = module
            fuzzy = False
            fuzzy_module = False
            try:
                master_log = master_results[master_plan_key][module][variant]
                matched_variant = variant
            except KeyError:
                # Exact variant match failed — try fuzzy matching where one
                # variant set is a subset of the other (handles added/removed
                # variant parameters across branches).
                try:
                    matched_variant = find_fuzzy_variant_match(
                        master_results[master_plan_key][module], variant)
                    if matched_variant is not None:
                        master_log = master_results[master_plan_key][module][matched_variant]
                        fuzzy = True
                except KeyError:
                    pass

            # If module name not found at all, try fuzzy module matching
            if (master_log is None
                    and master_plan_key in master_results
                    and module not in master_results.get(master_plan_key, {})):
                fuzzy_matched_module = find_fuzzy_module_match(
                    master_results[master_plan_key], module)
                if fuzzy_matched_module is not None:
                    try:
                        master_log = master_results[master_plan_key][fuzzy_matched_module][variant]
                        matched_module = fuzzy_matched_module
                        matched_variant = variant
                        fuzzy_module = True
                    except KeyError:
                        # Exact variant failed on fuzzy module — try fuzzy variant too
                        matched_variant = find_fuzzy_variant_match(
                            master_results[master_plan_key][fuzzy_matched_module], variant)
                        if matched_variant is not None:
                            master_log = master_results[master_plan_key][fuzzy_matched_module][matched_variant]
                            matched_module = fuzzy_matched_module
                            fuzzy_module = True
                            fuzzy = True

            if master_log is not None:
                del master_results[master_plan_key][matched_module][matched_variant]
                any_fuzzy = fuzzy_plan or fuzzy_module or fuzzy
                output = compare(master_log, log)
                if output != None or any_fuzzy:
                    differences=True
                    print("Plan: "+test_plan)
                    print("Module: "+module)
                    print("Variant: "+pretty_variant(variant))
                    if fuzzy_plan:
                        print(yellow("(fuzzy plan match — reference plan: "+master_plan_key+")"))
                    if fuzzy_module:
                        print(yellow("(fuzzy module match — reference had: "+matched_module+")"))
                    if fuzzy:
                        print(yellow("(fuzzy variant match — differences:"))
                        print(pretty_variant_diff(variant, matched_variant))
                        print(yellow(")"))
                    print("reference log: "+get_url(master_log))
                    print("new log: "+get_url(log))
                    if output != None:
                        print("Diff output:\n"+output)
                    else:
                        print(yellow("(no condition diff, but fuzzy matching was required)\n"))
            else:
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
