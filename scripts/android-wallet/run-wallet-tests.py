#!/usr/bin/env python3
"""Run conformance suite wallet test plans against an Android wallet in an emulator, driven over adb.

Uses only the standard library. Needs a booted emulator with the wallet installed (see start-emulator.sh),
CONFORMANCE_SERVER and CONFORMANCE_TOKEN. Screenshots, screen texts and the wallet's logcat go to ARTIFACT_DIR,
with results.json listing every module run.

    run-wallet-tests.py <vci-config.json> <vp-config.json> <expected.json>
    run-wallet-tests.py --check-expected <expected.json>

The VCI plan runs first, so that the wallet holds the PIDs the VP plan asks for. Each module's outcome is
the suite's status and result plus what the wallet showed; the run fails when any outcome differs from
expected.json. results.json is rewritten after every module and logcat is saved however the run ends;
each plan's log export zip goes to EXPORT_DIR (default: the current directory) for the compare job.
--check-expected only validates expected.json, so the job can refuse an empty baseline before booting
the emulator. RECORD_BASELINE=1 lets a run through anyway and passes it; its expected-from-run.json
artifact is the baseline to commit.
"""
import base64
import json
import os
import re
import shlex
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ElementTree

SERVER = os.environ.get("CONFORMANCE_SERVER", "").rstrip("/") + "/"
TOKEN = os.environ.get("CONFORMANCE_TOKEN", "")
PIN = os.environ.get("WALLET_PIN", "147258")
ARTIFACTS = os.environ.get("ARTIFACT_DIR", "android-wallet-artifacts")
EXPORT_DIR = os.environ.get("EXPORT_DIR", ".")
# a baseline-recording run (RECORD_BASELINE=1, exactly) accepts an empty expected.json and passes whatever the outcomes are
RECORD_BASELINE = os.environ.get("RECORD_BASELINE") == "1"
WALLET_PACKAGE = os.environ.get("WALLET_PACKAGE", "eu.europa.ec.euidi")

# every module of this plan uses credential encryption, which the EC reference wallet requires
VCI_PLAN = ("oid4vci-1_0-wallet-haip-encrypted-test-plan", {
    "credential_format": "sd_jwt_vc",
    "vci_authorization_code_flow_variant": "issuer_initiated",
    "grant_management": "disabled",
    "vci_credential_offer_variant": "by_value",
})
VP_PLAN = ("oid4vp-1final-wallet-haip-test-plan", {
    "credential_type": "eudi_pid",
    "credential_format": "sd_jwt_vc",
    "response_mode": "direct_post.jwt",
})

WALLET_ERROR = "Oups! Something went wrong"
BLOCKED = "Presentation blocked"
# The demo flavour presents each PID instance only once (CredentialPolicy.OnceOnly), and the suite's
# issuance module issues one, so the VP plan needs a PID issued for every module that presents one
EXTRA_PIDS = int(os.environ.get("EXTRA_PIDS", "14"))
NOT_IN_WALLET = "The requested document is not available in your EUDI Wallet."


# --- suite API ---------------------------------------------------------------------------------

def api(method, path, params=None, body=None, content_type="application/json"):
    url = SERVER + "api/" + path + ("?" + urllib.parse.urlencode(params) if params else "")
    request = urllib.request.Request(url, method=method, data=body,
                                     headers={"Authorization": "Bearer " + TOKEN, "Content-Type": content_type})
    # retry network errors (not HTTP error responses) a few times; a run takes long enough to meet one
    for attempt in range(5):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return json.loads(response.read() or "null")
        except urllib.error.HTTPError as e:
            # a 5xx is usually the server restarting or overloaded; anything else is a real answer
            if e.code < 500 or attempt == 4:
                raise
            time.sleep(15)
        except (urllib.error.URLError, TimeoutError, ConnectionError):
            if attempt == 4:
                raise
            time.sleep(5)


def export_plan(plan_id):
    """Save the plan's log export zip where compare_android_wallet (and compare-results.py) look for it:
    the current directory, under the server's file name."""
    request = urllib.request.Request(SERVER + "api/plan/export/" + plan_id, headers={"Authorization": "Bearer " + TOKEN})
    with urllib.request.urlopen(request, timeout=300) as response:
        match = re.findall('filename="(.+)"', response.headers.get("content-disposition", ""))
        if not match:
            raise SystemExit("plan export for " + plan_id + " carried no file name")
        with open(os.path.join(EXPORT_DIR, match[0]), "wb") as f:
            f.write(response.read())
    print("  exported", match[0])


def create_plan(plan, config):
    name, variant = plan
    plan_id = api("POST", "plan", {"planName": name, "variant": json.dumps(variant)}, json.dumps(config).encode())["id"]
    print("plan", name, SERVER + "plan-detail.html?plan=" + plan_id)
    return api("GET", "plan/" + plan_id)


def start_module(plan_id, entry):
    module_id = api("POST", "runner", {"test": entry["testModule"], "plan": plan_id,
                                       "variant": json.dumps(entry["variant"])})["id"]
    print("module", entry["testModule"], SERVER + "log-detail.html?log=" + module_id)
    return module_id


def module_status(module_id):
    info = api("GET", "info/" + module_id)
    return info.get("status"), info.get("result")


def wait_for_module(module_id, timeout):
    deadline = time.time() + timeout
    while True:
        status, result = module_status(module_id)
        if status in ("FINISHED", "INTERRUPTED") or time.time() > deadline:
            return status, result
        time.sleep(3)


def next_wallet_url(module_id, timeout=60):
    """The first URL the module wants a wallet to open that it has not opened yet, or None."""
    deadline = time.time() + timeout
    while True:
        browser = api("GET", "runner/browser/" + module_id)
        pending = [u for u in browser.get("urls") or [] if u not in (browser.get("visited") or [])]
        if pending:
            return pending[0]
        if time.time() > deadline or module_status(module_id)[0] in ("FINISHED", "INTERRUPTED"):
            return None
        time.sleep(2)


def findings(module_id):
    """The module's FAILURE and WARNING log entries, as 'condition: message'."""
    return [e.get("src", "") + ": " + str(e.get("msg", ""))[:300]
            for e in api("GET", "log/" + module_id) if e.get("result") in ("FAILURE", "WARNING")]


def upload_screenshot(module_id, png):
    """Fill the module's open image placeholders with a screenshot of the wallet's error screen."""
    data = ("data:image/png;base64," + base64.b64encode(png).decode()).encode()
    filled = 0
    for entry in api("GET", "log/" + module_id + "/images"):
        if entry.get("upload") and not entry.get("img"):
            api("POST", "log/" + module_id + "/images/" + entry["upload"], body=data, content_type="text/plain")
            filled += 1
    return filled


# --- wallet over adb ---------------------------------------------------------------------------

def adb_run(*args, timeout=60):
    return subprocess.run(["adb", *args], capture_output=True, timeout=timeout, check=False)


def adb(*args, timeout=60):
    return adb_run(*args, timeout=timeout).stdout


UI_DUMP = "/sdcard/ui.xml"


def screen():
    """The visible UI as a list of (text, content-desc, class, (x, y) centre).

    The previous dump is removed first, so a failed dump yields an empty screen rather than the last one.
    """
    nodes = []
    for _ in range(5):
        adb("shell", "rm", "-f", UI_DUMP)
        if adb_run("shell", "uiautomator", "dump", UI_DUMP).returncode == 0:
            try:
                root = ElementTree.fromstring(adb("exec-out", "cat", UI_DUMP))
            except ElementTree.ParseError:
                root = None
            if root is not None and root.tag == "hierarchy":
                for node in root.iter("node"):
                    bounds = [int(n) for n in re.findall(r"\d+", node.get("bounds", ""))]
                    centre = ((bounds[0] + bounds[2]) // 2, (bounds[1] + bounds[3]) // 2) if len(bounds) == 4 else None
                    nodes.append((node.get("text", ""), node.get("content-desc", ""), node.get("class", ""), centre))
                return nodes
        time.sleep(1)
    return nodes


def texts(nodes):
    return [t for t, _, _, _ in nodes if t]


def shows(nodes, fragment):
    return any(fragment in t for t in texts(nodes))


def find(nodes, label):
    for text, desc, _, centre in nodes:
        if label in (text, desc):
            return centre
    return None


def tap(centre):
    adb("shell", "input", "tap", str(centre[0]), str(centre[1]))


def type_text(value):
    adb("shell", "input", "text", value)


def back():
    adb("shell", "input", "keyevent", "4")


def evidence(label):
    os.makedirs(ARTIFACTS, exist_ok=True)
    png = adb("exec-out", "screencap", "-p")
    with open(os.path.join(ARTIFACTS, label + ".png"), "wb") as f:
        f.write(png)
    with open(os.path.join(ARTIFACTS, label + ".txt"), "w") as f:
        f.write("\n".join(texts(screen())) + "\n")
    return png


def open_in_wallet(url):
    adb("shell", "am start -a android.intent.action.VIEW -d " + shlex.quote(url))


# Android permission prompts, "isn't responding" dialogs on a loaded emulator, and Chrome's first-run
# screens, which a fresh emulator shows when the wallet first opens the authorization endpoint in the browser
DISMISS_LABELS = ("Don’t allow", "Don't allow", "Allow", "Wait", "Use without an account", "No thanks", "No, thanks",
                  "Accept & continue", "Got it")


def dismiss_system_dialogs(nodes):
    for label in DISMISS_LABELS:
        centre = find(nodes, label)
        if centre:
            tap(centre)
            return True
    return False


def pin_requested(nodes):
    return any(t in ("Welcome back", "Type a PIN", "Confirm PIN", "Enter your PIN") for t in texts(nodes)) \
        or any(t.startswith("Please enter your PIN") for t in texts(nodes))


def close_result_screen(nodes):
    centre = find(nodes, "Close")
    if centre:
        tap(centre)
    else:
        back()


def onboard():
    """Set the PIN on a freshly installed wallet; a no-op if it is already set up."""
    adb("shell", "monkey", "-p", WALLET_PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    deadline = time.time() + 180
    while time.time() < deadline:
        nodes = screen()
        if dismiss_system_dialogs(nodes):
            continue
        shown = texts(nodes)
        if "EU Digital Identity Wallet" in shown and find(nodes, "Continue"):
            tap(find(nodes, "Continue"))
        elif shows(nodes, WALLET_ERROR) or shows(nodes, "Document issued") or shows(nodes, BLOCKED):
            # a result screen left over from an earlier run
            close_result_screen(nodes)
        elif pin_requested(nodes):
            type_text(PIN)
        elif find(nodes, "GO TO HOME"):
            tap(find(nodes, "GO TO HOME"))
        elif "Welcome" in shown or "Documents" in shown or "Home" in shown:
            print("wallet onboarded")
            return
        time.sleep(3)
    evidence("onboard-timeout")
    raise SystemExit("wallet onboarding did not reach the home screen within 180 s")


def wallet_handles_offer(url, label):
    """Open a credential offer and accept it. Returns ('issued' | 'deferred' | 'error' | 'timeout', screen text)."""
    open_in_wallet(url)
    deadline = time.time() + 180
    accepted = False
    while time.time() < deadline:
        nodes = screen()
        if dismiss_system_dialogs(nodes):
            continue
        if pin_requested(nodes):
            type_text(PIN)
        elif shows(nodes, "Document issued") or shows(nodes, "Documents issued"):
            evidence(label + "-issued")
            close_result_screen(nodes)
            return "issued", ""
        elif shows(nodes, WALLET_ERROR):
            evidence(label + "-error")
            close_result_screen(nodes)
            return "error", " / ".join(t for t in texts(nodes) if t != WALLET_ERROR)
        elif shows(nodes, "In progress!") and find(nodes, "OK"):
            # deferred issuance: the wallet fetches the credential later
            evidence(label + "-deferred")
            tap(find(nodes, "OK"))
            return "deferred", ""
        elif not accepted and shows(nodes, "Issuance request") and find(nodes, "Accept"):
            tap(find(nodes, "Accept"))
            accepted = True
        time.sleep(2)
    evidence(label + "-timeout")
    back()
    return "timeout", ""


def wallet_handles_request(url, label):
    """Open a presentation request. Shares when the wallet offers to, else captures the wallet's screen.

    Returns (outcome, screen text, screenshot or None); outcome is 'shared', 'error', 'blocked',
    'not-in-wallet', 'share-failed' or 'timeout'.
    """
    open_in_wallet(url)
    deadline = time.time() + 120
    shared = False
    pin_typed = False
    while time.time() < deadline:
        nodes = screen()
        if dismiss_system_dialogs(nodes):
            continue
        shown = " / ".join(texts(nodes))
        if pin_requested(nodes) or (shared and not pin_typed and any("EditText" in c for _, _, c, _ in nodes)):
            type_text(PIN)
            pin_typed = shared
        elif shared and find(nodes, "Close"):
            # the wallet opens the direct_post redirect_uri only when Close is tapped on its result screen
            outcome = "shared" if shows(nodes, "successful") else "share-failed"
            evidence(label + "-" + outcome)
            tap(find(nodes, "Close"))
            return outcome, shown, None
        elif shows(nodes, WALLET_ERROR):
            png = evidence(label + "-error")
            close_result_screen(nodes)
            return "error", shown, png
        elif shows(nodes, BLOCKED):
            png = evidence(label + "-blocked")
            close_result_screen(nodes)
            return "blocked", shown, png
        elif shows(nodes, NOT_IN_WALLET):
            png = evidence(label + "-not-in-wallet")
            back()
            return "not-in-wallet", shown, png
        elif not shared and find(nodes, "Share"):
            evidence(label + "-request")
            tap(find(nodes, "Share"))
            shared = True
        time.sleep(2)
    png = evidence(label + "-timeout")
    back()
    return "timeout", "", png


# --- plans -------------------------------------------------------------------------------------

def label_for(entry):
    return entry["testModule"].replace("oid4vci-1_0-wallet-test-", "vci-").replace("oid4vp-1final-wallet-", "vp-")


def run_vci_module(plan_id, entry):
    module_id = start_module(plan_id, entry)
    label = label_for(entry)
    url = next_wallet_url(module_id)
    wallet, detail = wallet_handles_offer(url, label) if url else ("no-url", "")
    if wallet == "deferred":
        # give the wallet's background check for deferred credentials a few chances, reopening the app
        for _ in range(5):
            status, result = wait_for_module(module_id, 60)
            if status in ("FINISHED", "INTERRUPTED"):
                break
            adb("shell", "monkey", "-p", WALLET_PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
            if pin_requested(screen()):
                type_text(PIN)
    status, result = wait_for_module(module_id, 90)
    return module_id, wallet, detail, status, result


def run_vp_module(plan_id, entry):
    module_id = start_module(plan_id, entry)
    label = label_for(entry)
    wallet, detail, png = "no-url", "", None
    # a module can send the wallet more than one request (for example a retry after an error response)
    for round_ in range(3):
        url = next_wallet_url(module_id, timeout=30 if round_ else 60)
        if not url:
            break
        wallet, detail, png = wallet_handles_request(url, label + ("-" + str(round_ + 1) if round_ else ""))
        if module_status(module_id)[0] in ("FINISHED", "INTERRUPTED"):
            break
    if png and upload_screenshot(module_id, png):
        print("  uploaded the wallet's screen for review")
    status, result = wait_for_module(module_id, 60)
    return module_id, wallet, detail, status, result


RESULTS = []


def save_results():
    with open(os.path.join(ARTIFACTS, "results.json"), "w") as f:
        json.dump(RESULTS, f, indent=1)
    # the same outcomes in expected.json's shape, ready to commit as the baseline
    with open(os.path.join(ARTIFACTS, "expected-from-run.json"), "w") as f:
        json.dump({r["key"]: outcome(r) for r in RESULTS}, f, indent=1, sort_keys=True)


def record(result):
    RESULTS.append(result)
    save_results()


def run_plan(plan, config, run_module, skip):
    plan_doc = create_plan(plan, config)
    for entry in plan_doc["modules"]:
        key = entry["testModule"] + " " + json.dumps(entry["variant"], sort_keys=True)
        reason = skip(entry)
        if reason:
            record({"key": key, "wallet": "not-run", "detail": reason})
            continue
        module_id, wallet, detail, status, result = run_module(plan_doc["_id"], entry)
        found = findings(module_id)
        print("  wallet:", wallet, detail[:200], "| suite:", status, result)
        for f in found:
            print("    " + f[:250])
        record({"key": key, "module_id": module_id, "wallet": wallet, "detail": detail,
                "status": status, "result": result, "findings": found})
    return plan_doc


def issue_extra_pids(plan_doc, count):
    """Run the plain issuance module again so the wallet holds a PID for every VP module."""
    entry = next(m for m in plan_doc["modules"]
                 if m["testModule"] == "oid4vci-1_0-wallet-test-credential-issuance" and skip_vci(m) is None
                 and m["variant"].get("vci_credential_issuance_mode") == "immediate")
    issued = 0
    for _ in range(count):
        if run_vci_module(plan_doc["_id"], entry)[1] == "issued":
            issued += 1
    print("issued %d of %d extra PIDs" % (issued, count))


def skip_vci(entry):
    if entry["variant"].get("vci_credential_encryption") == "plain":
        return "the wallet requires an encrypted credential response"
    if os.environ.get("SKIP_DEFERRED") and entry["variant"].get("vci_credential_issuance_mode") == "deferred":
        return "deferred issuance skipped (SKIP_DEFERRED)"
    return None


def load_config(path, alias):
    return json.loads(open(path).read().replace("{ALIAS}", alias))


EXPECTED_FIELDS = {"wallet", "status", "result"}


def outcome(r):
    return {"wallet": r["wallet"], "status": r.get("status"), "result": r.get("result")}


def compare(results, expected):
    """Lines describing every result that differs from expected.json (keyed like results.json)."""
    problems = []
    for r in results:
        want = expected.get(r["key"])
        got = outcome(r)
        if want is None:
            problems.append("no expectation for " + r["key"] + ": got " + json.dumps(got))
        elif want != got:
            problems.append(r["key"] + ": expected " + json.dumps(want) + ", got " + json.dumps(got))
    for key in set(expected) - {r["key"] for r in results}:
        problems.append("expected module did not run: " + key)
    return problems


def load_expected(path):
    """expected.json as a dict; an empty or malformed file is refused so a run cannot silently gate nothing."""
    try:
        expected = json.load(open(path))
    except ValueError as e:
        raise SystemExit("%s is not valid JSON: %s" % (path, e))
    if not isinstance(expected, dict):
        raise SystemExit("%s must hold an object keyed by module and variants" % path)
    if not expected and not RECORD_BASELINE:
        raise SystemExit("%s holds no expectations; run once with RECORD_BASELINE=1 and commit "
                         "the run's expected-from-run.json artifact" % path)
    for key, want in expected.items():
        if not isinstance(want, dict) or set(want) != EXPECTED_FIELDS:
            raise SystemExit("%s: entry %r must give exactly %s" % (path, key, sorted(EXPECTED_FIELDS)))
    return expected


def main():
    if len(sys.argv) == 3 and sys.argv[1] == "--check-expected":
        expected = load_expected(sys.argv[2])
        print("%d expected modules in %s" % (len(expected), sys.argv[2]))
        return
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    vci_config_path, vp_config_path, expected_path = sys.argv[1:]
    expected = load_expected(expected_path)
    if SERVER == "/" or not TOKEN:
        raise SystemExit("CONFORMANCE_SERVER and CONFORMANCE_TOKEN must be set")
    run = os.environ.get("CI_JOB_ID", str(int(time.time())))
    os.makedirs(ARTIFACTS, exist_ok=True)
    try:
        onboard()
        vci_plan = run_plan(VCI_PLAN, load_config(vci_config_path, "android-ci-vci-" + run), run_vci_module, skip_vci)
        # before the extra PIDs: the export holds each module's latest instance, and it must be the one results.json describes
        export_plan(vci_plan["_id"])
        issue_extra_pids(vci_plan, EXTRA_PIDS)
        vp_plan = run_plan(VP_PLAN, load_config(vp_config_path, "android-ci-vp-" + run), run_vp_module, lambda e: None)
        export_plan(vp_plan["_id"])
    finally:
        save_results()
        with open(os.path.join(ARTIFACTS, "logcat.txt"), "wb") as f:
            f.write(adb("logcat", "-d", timeout=120))
    problems = compare(RESULTS, expected)
    print("\n%d modules, %d unexpected outcomes" % (len(RESULTS), len(problems)))
    for p in problems:
        print("UNEXPECTED", p)
    if RECORD_BASELINE:
        print("RECORD_BASELINE set: commit the expected-from-run.json artifact as expected-ec-wallet.json")
        return
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
