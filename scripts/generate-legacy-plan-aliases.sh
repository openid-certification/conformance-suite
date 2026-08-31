#!/usr/bin/env bash
#
# Regenerates src/main/resources/statistics/legacy-plan-families.properties: the map from
# retired or renamed test plan names to the spec family they belonged to, mined from this
# repository's own git history.
#
# The admin statistics page attributes a test run to a spec family by looking its plan name
# up in the live registry (SpecFamilyResolver). Runs recorded under a plan name that no
# longer exists - a plan that was deleted, or renamed, e.g. "fapi-rw-id2-test-plan" ->
# "fapi1-advanced-final-test-plan" - fall into "Other / retired". This script recovers those
# names from history so they can be charted under the family they were run for.
#
# How it works: every *.java file under src/main/java that ever contained @PublishTestPlan is
# collected, every revision of it is read, and every testPlanName literal it ever declared is
# recorded. The family comes from the specFamily attribute of the same annotation (added to
# @PublishTestPlan in March 2026, so most retired plans predate it), then from the last
# specFamily the same file declared, then from the rule table in this script. Names the
# running suite still publishes are excluded - the alias map is only consulted after the
# registry, so listing them there would be dead weight - and names no rule maps are left out
# and counted, by package, in the generated file's header.
#
# Run it after retiring or renaming a test plan. It is not part of the build or of CI: it
# walks all of git history and its output only changes when plan names do.
#
# It walks origin/master and HEAD only - deliberately NOT --all. Clones of this repository
# often have fork remotes whose plans never existed here, and names from a fork must not end
# up in our alias map. Walking our own history also makes the output reproducible across
# clones, which --all was not.
#
# Usage (from anywhere):
#   scripts/generate-legacy-plan-aliases.sh                 # rewrite the properties file
#   scripts/generate-legacy-plan-aliases.sh --dry-run       # print it instead
#   scripts/generate-legacy-plan-aliases.sh --report t.tsv  # also dump every name it found:
#                                                           # name, family, constant,
#                                                           # provenance, source paths
#
# Requires: git, python3 (standard library only). Takes about a minute.

set -euo pipefail

REPO_ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
export REPO_ROOT

exec python3 - "$@" <<'PYTHON'
"""Mines every test plan name this repository ever published out of git history."""

import os
import re
import subprocess
import sys
from collections import defaultdict

REPO = os.environ["REPO_ROOT"]
SOURCE_ROOT = "src/main/java/"
TEST_PLAN_JAVA = "src/main/java/net/openid/conformance/plan/TestPlan.java"
OUTPUT = "src/main/resources/statistics/legacy-plan-families.properties"
COMMAND = "scripts/generate-legacy-plan-aliases.sh"

# how many unmapped names a package has to be under before the header names them one by one
NAME_UNMAPPED_UP_TO = 5

# Names whose mapping this script must not silently lose, checked on every run: one per rule
# that took an argument to get right, plus two of the names that have to stay out. None means
# "must not be in the properties file".
SELF_CHECK = {
    "fapi-rw-id2-test-plan": "fapi1Advanced",
    "fapi-r-test-plan": "fapi1Advanced",
    "ob-code-id-token-with-mtls-test-plan": "fapi1Advanced",
    "fapi-ciba-poll-test-plan": "fapiCiba",
    "fapi2-baseline-id2-test-plan": "fapi2SecurityProfile",
    "fapi2-advanced-id1-test-plan": "fapi2MessageSigning",
    "oidcc-client-back-channel-logout-rp-basic-test-plan": "oidccLogout",
    "oidcc-client-rp-session-management-rp-basic-test-plan": "oidccSessionManagement",
    "oidcc-basic-certifcation-test-plan": "oidcc",
    "Account api test": None,
    "fvp-payments-e2e_test-plan-v3": None,
}

ANNOTATION = "@PublishTestPlan"
NAME_RE = re.compile(r'testPlanName\s*=\s*"([^"]*)"')
FAMILY_RE = re.compile(r"specFamily\s*=\s*(?:TestPlan\s*\.\s*)?SpecFamilyNames\s*\.\s*(\w+)")
CONSTANT_RE = re.compile(r'String\s+(\w+)\s*=\s*"([^"]*)"\s*;')

# Plan names that predate the specFamily attribute get their family from the first rule here
# that matches. A "name" rule is matched against the plan name, a "path" rule against the
# source path the name was last declared in, and the families are SpecFamilyNames constant
# names, resolved to their display strings below. None means "deliberately unmapped": the
# example package holds the framework's demo plan, which belongs to no spec family, so it is
# left out rather than filed under a family a later rule would give it.
#
# Name rules run before every path rule, the exclusion included, because a name outlives the
# file it was declared in: a plan that moved package keeps the name its runs were recorded
# under, so a name the table recognises beats the package it ended up in. Anchor them - an
# unanchored substring matches far too much ("ssf" is inside "BusinessFinancings", "vp-" is
# inside "fvp-payments").
RULES = (
    # where to match, the pattern to match, the SpecFamilyNames constant it means
    ("name", r"(^|[^a-z0-9])ciba([^a-z0-9]|$)", "fapiCiba"),
    ("name", r"logout", "oidccLogout"),
    ("name", r"session[-_ ]?management", "oidccSessionManagement"),
    ("name", r"message[-_ ]?signing", "fapi2MessageSigning"),
    # the FAPI2 "Advanced" profile was renamed to Message Signing, not to Security Profile:
    # the file that declared fapi2-advanced-id1-test-plan went on to declare
    # fapi2-message-signing-id1-test-plan, so this has to sit above the generic fapi2 rule
    ("name", r"^fapi[-_]?2[-_]advanced", "fapi2MessageSigning"),
    ("name", r"^fapi[-_]?2", "fapi2SecurityProfile"),
    ("name", r"^fapi[-_]?1", "fapi1Advanced"),
    ("name", r"^fapi([-_]|$)", "fapi1Advanced"),
    # the UK Open Banking plans, which ran the FAPI1 profile against an OB flavoured server
    ("name", r"^ob([-_]|$)", "fapi1Advanced"),
    ("name", r"^ekyc", "ekyc"),
    ("name", r"^(oid4)?vci", "oid4vci"),
    ("name", r"^(oid4)?vp([-_]|$)", "oid4vp"),
    ("name", r"federation", "federation"),
    ("name", r"^ssf", "ssf"),
    ("name", r"authzen", "authzen"),
    ("name", r"^oidcc", "oidcc"),
    # the sample plan the framework ships as an example is not a spec family
    ("path", r"/example/", None),
    ("path", r"/fapiciba/", "fapiCiba"),
    ("path", r"/fapi2[a-z0-9]*/", "fapi2SecurityProfile"),
    ("path", r"/(fapi|fapir|fapirwid2|fapi1advancedfinal[a-z0-9]*)/", "fapi1Advanced"),
    ("path", r"/openbanking[-_]?(deprecated)?/", "fapi1Advanced"),
    ("path", r"/ekyc/", "ekyc"),
    ("path", r"/vci[a-z0-9]*/", "oid4vci"),
    ("path", r"/(vp|vpverifier|vpid[0-9a-z]*|vcpresentation)/", "oid4vp"),
    ("path", r"/federation/", "federation"),
    ("path", r"/ssf/", "ssf"),
    ("path", r"/conformance/openid/", "oidcc"),
)


def git(*args):
    """Runs git in the repository and returns its stdout, decoded."""
    return subprocess.run(["git", "-C", REPO, *args], check=True,
        capture_output=True).stdout.decode("utf-8", "replace")


def rev_exists(rev):
    """@return whether the clone has this revision."""
    return subprocess.run(["git", "-C", REPO, "rev-parse", "--verify", "--quiet", rev + "^{commit}"],
        capture_output=True).returncode == 0


def annotation_blocks(text):
    """Yields the parenthesised body of every @PublishTestPlan annotation in a java file."""
    at = text.find(ANNOTATION)
    while at != -1:
        start = text.find("(", at)
        if start == -1:
            return
        depth = 0
        i = start
        in_string = False
        while i < len(text):
            c = text[i]
            if in_string:
                if c == "\\":
                    i += 2
                    continue
                if c == '"':
                    in_string = False
            elif c == '"':
                in_string = True
            elif c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
                if depth == 0:
                    yield text[start:i + 1]
                    break
            i += 1
        at = text.find(ANNOTATION, i)


# every history walk here: our own history (origin/master plus whatever HEAD adds, never other
# remotes - see the header), renames left undetected so that both sides of one are listed, and
# no history simplification so that a name that only ever existed on a merged side branch is
# still found
def our_refs():
    """@return the revisions to walk: origin/master when the clone has it, plus HEAD."""
    base = next((r for r in ("origin/master", "master") if rev_exists(r)), None)
    return ([base] if base else []) + ["HEAD"]


WALK = ("log", "--full-history", "--no-renames")


def candidate_paths():
    """@return every java source path that any commit's diff mentions a plan name in.

    -S selects the commits whose diff changes how often the string occurs, and --no-renames
    makes a rename look like a delete plus an add so that the new path is listed too. The
    result is a superset - the other files of those commits come along - which the blob scan
    below then filters by content.
    """
    paths = set()
    for needle in ("testPlanName", ANNOTATION):
        listing = git(*WALK, *our_refs(), "-S", needle, "--name-only", "--format=", "--", "*.java")
        for line in listing.splitlines():
            path = line.strip()
            if path.startswith(SOURCE_ROOT) and path.endswith(".java"):
                paths.add(path)
    return sorted(paths)


def revisions(paths):
    """@return (committer timestamp, commit, path) for every revision of every candidate."""
    listing = git(*WALK, *our_refs(), "--diff-filter=AMR", "--name-only", "--format=C %H %ct", "--", *paths)
    revs = []
    commit = timestamp = None
    for line in listing.splitlines():
        if line.startswith("C "):
            _, commit, timestamp = line.split()
        elif line and commit:
            revs.append((int(timestamp), commit, line))
    return revs


def blob_ids(revs):
    """@return the blob id of each revision, or None where the path was not a file."""
    spec = "".join(f"{commit}:{path}\n" for _, commit, path in revs).encode()
    out = subprocess.run(["git", "-C", REPO, "cat-file", "--batch-check=%(objectname) %(objecttype)"],
        input=spec, capture_output=True, check=True).stdout.decode()
    ids = []
    for line in out.splitlines():
        parts = line.split()
        ids.append(parts[0] if len(parts) >= 2 and parts[1] == "blob" else None)
    if len(ids) != len(revs):
        raise SystemExit(f"cat-file answered {len(ids)} of {len(revs)} revisions")
    return ids


def blob_contents(ids):
    """@return the text of each blob; deduplicated, because a file's revisions repeat blobs."""
    wanted = sorted({blob for blob in ids if blob})
    spec = "".join(blob + "\n" for blob in wanted).encode()
    # communicate() rather than a hand rolled write/read loop: git blocks on a full stdout
    # pipe long before a batch this size has been written to it
    out = subprocess.run(["git", "-C", REPO, "cat-file", "--batch"],
        input=spec, capture_output=True, check=True).stdout
    contents = {}
    at = 0
    for blob in wanted:
        end = out.index(b"\n", at)
        size = int(out[at:end].split()[2])
        contents[blob] = out[end + 1:end + 1 + size].decode("utf-8", "replace")
        at = end + 1 + size + 1
    return contents


def head_plan_names():
    """@return the plan names HEAD publishes, which the alias map must not repeat."""
    names = set()
    for line in git("grep", "-l", ANNOTATION, "HEAD", "--", SOURCE_ROOT).splitlines():
        path = line.split(":", 1)[1]
        for block in annotation_blocks(git("show", f"HEAD:{path}")):
            found = NAME_RE.search(block)
            if found:
                names.add(found.group(1))
    return names


def family_display_names():
    """@return the SpecFamilyNames constants of the current TestPlan.java, by constant name."""
    source = git("show", f"HEAD:{TEST_PLAN_JAVA}")
    start = source.find("interface SpecFamilyNames")
    end = source.find("}", start)
    if start == -1 or end == -1:
        raise SystemExit(f"no SpecFamilyNames interface in {TEST_PLAN_JAVA}")
    families = dict(CONSTANT_RE.findall(source[start:end]))
    if not families:
        raise SystemExit(f"no constants in the SpecFamilyNames interface of {TEST_PLAN_JAVA}")
    return families


def rule_family(plan_name, latest_path):
    """@return the SpecFamilyNames constant the rule table gives a plan, or None for both a
    plan the table deliberately leaves out and one no rule matches - which is the same answer,
    because either way the name does not go in the properties file."""
    lowered = plan_name.lower()
    for where, pattern, constant in RULES:
        if re.search(pattern, lowered if where == "name" else latest_path.lower()):
            return constant
    return None


def mine():
    """@return every plan name history holds, with the family and provenance of each."""
    paths = candidate_paths()
    print(f"candidate source paths: {len(paths)}", file=sys.stderr)
    revs = revisions(paths)
    ids = blob_ids(revs)
    contents = blob_contents(ids)
    print(f"revisions: {len(revs)} in {len(contents)} distinct blobs", file=sys.stderr)

    declared = {}          # plan name -> (timestamp, family constant) declared on the plan
    name_paths = defaultdict(dict)   # plan name -> path -> the last time it was declared there
    file_families = {}     # path -> (timestamp, the family constants that revision declared)
    for (timestamp, _, path), blob in zip(revs, ids):
        text = contents.get(blob)
        if text is None or ANNOTATION not in text:
            continue
        families = set()
        for block in annotation_blocks(text):
            found = NAME_RE.search(block)
            if not found:
                continue
            name = found.group(1)
            name_paths[name][path] = max(timestamp, name_paths[name].get(path, 0))
            family = FAMILY_RE.search(block)
            if family:
                families.add(family.group(1))
                previous = declared.get(name)
                if previous is None or previous[0] < timestamp:
                    declared[name] = (timestamp, family.group(1))
        if families:
            previous = file_families.get(path)
            if previous is None or previous[0] < timestamp:
                file_families[path] = (timestamp, families)

    mined = {}
    for name, paths_of_name in name_paths.items():
        # most recently declared in first: the name's last home is the one that describes it
        paths = sorted(paths_of_name, key=lambda path: (-paths_of_name[path], path))
        if name in declared:
            mined[name] = (declared[name][1], "declared", paths)
            continue
        # the plan lost its name before specFamily existed: take the family the file it lived
        # in ended up declaring, as long as that is unambiguous
        inherited = set()
        for path in paths:
            if path in file_families:
                inherited |= file_families[path][1]
        if len(inherited) == 1:
            mined[name] = (inherited.pop(), "inherited", paths)
            continue
        guessed = rule_family(name, paths[0])
        mined[name] = (guessed, "rule" if guessed else "unmapped", paths)
    return mined


def main():
    dry_run, report = options(sys.argv[1:])

    mined = mine()
    families = family_display_names()
    registry = head_plan_names()
    revision = git("rev-parse", "HEAD").strip()

    aliases = {}
    unmapped = defaultdict(list)
    unmapped_total = 0
    for name in sorted(mined):
        constant, _, paths = mined[name]
        if name in registry:
            continue
        display = families.get(constant) if constant else None
        if display is None:
            unmapped[package(paths[0])].append(name)
            unmapped_total += 1
            continue
        aliases[name] = display

    print(f"plan names in history: {len(mined)}; still published: {len(registry)}; "
        f"aliased: {len(aliases)}; unmapped: {unmapped_total}", file=sys.stderr)

    lines = [
        "# Retired and renamed test plan names, and the spec family they belonged to.",
        "#",
        "# Consulted by SpecFamilyResolver after the live registry, so that the statistics page",
        "# can chart a run recorded under a plan name the suite no longer publishes. Generated",
        "# from this repository's own history (origin/master and HEAD) at the revision below,",
        "# never from other remotes - do not edit by hand:",
        "#",
        f"#   {COMMAND}",
        "#",
        f"# git revision: {revision}",
        f"# plan names: {len(aliases)}",
    ]
    if unmapped:
        lines.append("#")
        lines.append(f"# {unmapped_total} more name(s) have no family, so their runs stay under "
            "\"Other / retired\".")
        lines.append("# By the package that last declared them (--report lists them one by one):")
        for group in sorted(unmapped, key=lambda name: (-len(unmapped[name]), name)):
            names = unmapped[group]
            lines.append(f"#   {group}: {len(names)}")
            # the big groups are whole retired suites and are named by their package; the odd
            # one or two left over are worth naming, in case they deserve a rule
            if len(names) <= NAME_UNMAPPED_UP_TO:
                lines.extend(f"#     {name}" for name in names)
    lines.append("")
    for name, display in sorted(aliases.items()):
        lines.append(f"{escape(name, True)}={escape(display, False)}")
    text = "\n".join(lines) + "\n"

    if report:
        with open(report, "w", encoding="utf-8") as handle:
            for name in sorted(mined):
                constant, provenance, paths = mined[name]
                if name in registry:
                    provenance = "published"
                handle.write(f"{name}\t{families.get(constant, '')}\t{constant or ''}\t{provenance}"
                    f"\t{';'.join(paths)}\n")
        print(f"wrote {report}", file=sys.stderr)

    check(aliases, families)

    if dry_run:
        sys.stdout.write(text)
        return
    destination = os.path.join(REPO, OUTPUT)
    os.makedirs(os.path.dirname(destination), exist_ok=True)
    with open(destination, "w", encoding="utf-8") as handle:
        handle.write(text)
    print(f"wrote {OUTPUT}", file=sys.stderr)


def check(aliases, families):
    """Fails the run if the mapping of a name in SELF_CHECK has changed, so that an edit to
    the rule table cannot quietly re-file - or drop - a plan whose family is settled."""
    wrong = []
    for name, constant in SELF_CHECK.items():
        expected = families.get(constant) if constant else None
        actual = aliases.get(name)
        if actual != expected:
            wrong.append(f"{name}: expected {expected or 'no entry'}, got {actual or 'no entry'}")
    if wrong:
        raise SystemExit("the rule table no longer maps these as SELF_CHECK says it must:\n  "
            + "\n  ".join(wrong))


def options(arguments):
    """@return (write to stdout rather than to the file, the report to write or None); exits
    with a usage message on an unknown option, a stray argument, or --report without a file."""
    dry_run = False
    report = None
    rest = list(arguments)
    while rest:
        argument = rest.pop(0)
        if argument == "--dry-run":
            dry_run = True
        elif argument == "--report":
            if not rest or rest[0].startswith("-"):
                raise SystemExit(f"--report needs a file to write the report to; see {COMMAND}")
            report = rest.pop(0)
        else:
            raise SystemExit(f"unexpected argument '{argument}'; see the header of {COMMAND}")
    return dry_run, report


def package(path):
    """@return the package a source path is in, to the depth that names a retired suite."""
    parts = path[len(SOURCE_ROOT):].split("/") if path.startswith(SOURCE_ROOT) else path.split("/")
    return "/".join(parts[:4])


def escape(text, key):
    """Escapes a java.util.Properties entry, which is read as ISO-8859-1 with backslash escapes.

    A key has to escape what would otherwise end it - whitespace, "=", ":" - and the comment
    markers; a value only has to escape the backslash. Both escape anything non ASCII, so that
    the file is byte for byte what Properties.load reads.
    """
    escaped = []
    for character in text:
        if character == "\\":
            escaped.append("\\\\")
        elif key and character in "=: \t#!":
            escaped.append("\\" + character)
        elif ord(character) > 126 or ord(character) < 32:
            escaped.append(f"\\u{ord(character):04x}")
        else:
            escaped.append(character)
    return "".join(escaped)


main()
PYTHON
