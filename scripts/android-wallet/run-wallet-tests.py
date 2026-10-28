#!/usr/bin/env python3
"""Run conformance suite wallet test plans against an Android wallet in an emulator, driven over adb.

Uses only the standard library. Needs a booted emulator with the wallet installed (see start-emulator.sh),
CONFORMANCE_SERVER and CONFORMANCE_TOKEN. Screenshots, screen texts and the wallet's logcat go to ARTIFACT_DIR,
with results.json listing every module run.

    run-wallet-tests.py <vci-config.json> <vp-config.json> <expected.json>

The VCI plan runs first, so that the wallet holds the PIDs the VP plan asks for. Each module's outcome is
the suite's status and result plus what the wallet showed; the run fails when any outcome differs from
expected.json.
"""
import base64
import json
import os
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request

SERVER = os.environ["CONFORMANCE_SERVER"].rstrip("/") + "/"
TOKEN = os.environ["CONFORMANCE_TOKEN"]
PIN = os.environ.get("WALLET_PIN", "147258")
ARTIFACTS = os.environ.get("ARTIFACT_DIR", "android-wallet-artifacts")
WALLET_PACKAGE = os.environ.get("WALLET_PACKAGE", "eu.europa.ec.euidi")

VCI_PLAN = ("oid4vci-1_0-wallet-haip-test-plan", {
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
NOT_IN_WALLET = "The requested document is not available in your EUDI Wallet."


# --- suite API ---------------------------------------------------------------------------------

def api(method, path, params=None, body=None, content_type="application/json"):
    url = SERVER + "api/" + path + ("?" + urllib.parse.urlencode(params) if params else "")
    request = urllib.request.Request(url, method=method, data=body,
                                     headers={"Authorization": "Bearer " + TOKEN, "Content-Type": content_type})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.loads(response.read() or "null")


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

def adb(*args, timeout=60):
    return subprocess.run(["adb", *args], capture_output=True, timeout=timeout, check=False).stdout


NODE = re.compile(r"<node [^>]*>")


def attr(node, name):
    match = re.search(name + r'="([^"]*)"', node)
    return match.group(1) if match else ""


def screen():
    """The visible UI as a list of (text, content-desc, class, (x, y) centre)."""
    xml = ""
    for _ in range(5):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        xml = adb("exec-out", "cat", "/sdcard/ui.xml").decode("utf-8", "replace")
        if "<hierarchy" in xml:
            break
        time.sleep(1)
    nodes = []
    for match in NODE.finditer(xml):
        node = match.group(0)
        bounds = [int(n) for n in re.findall(r"\d+", attr(node, "bounds"))]
        centre = ((bounds[0] + bounds[2]) // 2, (bounds[1] + bounds[3]) // 2) if len(bounds) == 4 else None
        nodes.append((attr(node, "text"), attr(node, "content-desc"), attr(node, "class"), centre))
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
    adb("shell", "am start -a android.intent.action.VIEW -d '" + url + "'")


# Android permission prompts and Chrome's first-run screens, which a fresh emulator shows when the
# wallet first opens the authorization endpoint in the browser
DISMISS_LABELS = ("Don’t allow", "Don't allow", "Allow", "Use without an account", "No thanks", "No, thanks",
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
    """Open a credential offer and accept it. Returns ('issued' | 'error' | 'timeout', screen text)."""
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
        elif not accepted and shows(nodes, "Issuance request") and find(nodes, "Accept"):
            tap(find(nodes, "Accept"))
            accepted = True
        time.sleep(2)
    evidence(label + "-timeout")
    back()
    return "timeout", ""


def wallet_handles_request(url, label):
    """Open a presentation request. Shares when the wallet offers to, else captures the wallet's screen.

    Returns (outcome, screen text, screenshot or None); outcome is 'shared', 'error', 'not-in-wallet',
    'share-failed' or 'timeout'.
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


def run_plan(plan, config, run_module, skip):
    plan_doc = create_plan(plan, config)
    results = []
    for entry in plan_doc["modules"]:
        key = entry["testModule"] + " " + json.dumps(entry["variant"], sort_keys=True)
        reason = skip(entry)
        if reason:
            results.append({"key": key, "wallet": "not-run", "detail": reason})
            continue
        module_id, wallet, detail, status, result = run_module(plan_doc["_id"], entry)
        print("  wallet:", wallet, detail[:200], "| suite:", status, result)
        results.append({"key": key, "module_id": module_id, "wallet": wallet, "detail": detail,
                        "status": status, "result": result})
    return results


def skip_vci(entry):
    if entry["variant"].get("vci_credential_encryption") == "plain":
        return "the wallet requires an encrypted credential response"
    return None


def load_config(path, alias):
    return json.loads(open(path).read().replace("{ALIAS}", alias))


def compare(results, expected):
    """Lines describing every result that differs from expected.json (keyed like results.json)."""
    problems = []
    for r in results:
        want = expected.get(r["key"])
        got = {"wallet": r["wallet"], "status": r.get("status"), "result": r.get("result")}
        if want is None:
            problems.append("no expectation for " + r["key"] + ": got " + json.dumps(got))
        elif any(want.get(k) != v for k, v in got.items() if k in want):
            problems.append(r["key"] + ": expected " + json.dumps(want) + ", got " + json.dumps(got))
    for key in set(expected) - {r["key"] for r in results}:
        problems.append("expected module did not run: " + key)
    return problems


def main():
    if len(sys.argv) != 4:
        raise SystemExit(__doc__)
    vci_config_path, vp_config_path, expected_path = sys.argv[1:]
    run = os.environ.get("CI_JOB_ID", str(int(time.time())))
    os.makedirs(ARTIFACTS, exist_ok=True)
    onboard()
    results = run_plan(VCI_PLAN, load_config(vci_config_path, "android-ci-vci-" + run), run_vci_module, skip_vci)
    results += run_plan(VP_PLAN, load_config(vp_config_path, "android-ci-vp-" + run), run_vp_module, lambda e: None)
    with open(os.path.join(ARTIFACTS, "results.json"), "w") as f:
        json.dump(results, f, indent=1)
    with open(os.path.join(ARTIFACTS, "logcat.txt"), "wb") as f:
        f.write(adb("logcat", "-d", timeout=120))
    problems = compare(results, json.load(open(expected_path)))
    print("\n%d modules, %d unexpected outcomes" % (len(results), len(problems)))
    for p in problems:
        print("UNEXPECTED", p)
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
