#!/usr/bin/env python3
"""Run conformance suite wallet test modules against an Android wallet in an emulator, driven over adb.

Uses only the standard library. Needs a booted emulator with the wallet installed (see start-emulator.sh),
CONFORMANCE_SERVER and CONFORMANCE_TOKEN. Screenshots, UI dumps and the wallet's logcat go to ARTIFACT_DIR.

    run-wallet-tests.py vci-issuance <config.json>
"""
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

VCI_HAIP_PLAN = "oid4vci-1_0-wallet-haip-test-plan"
VCI_PLAN_VARIANT = {
    "credential_format": "sd_jwt_vc",
    "vci_authorization_code_flow_variant": "issuer_initiated",
    "grant_management": "disabled",
    "vci_credential_offer_variant": "by_value",
}


# --- suite API ---------------------------------------------------------------------------------

def api(method, path, params=None, body=None, content_type="application/json"):
    url = SERVER + "api/" + path + ("?" + urllib.parse.urlencode(params) if params else "")
    request = urllib.request.Request(url, method=method, data=body,
                                     headers={"Authorization": "Bearer " + TOKEN, "Content-Type": content_type})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.loads(response.read() or "null")


def create_plan(plan_name, variant, config):
    return api("POST", "plan", {"planName": plan_name, "variant": json.dumps(variant)}, json.dumps(config).encode())


def start_module(plan_id, test_module, variant):
    return api("POST", "runner", {"test": test_module, "plan": plan_id, "variant": json.dumps(variant)})["id"]


def module_info(module_id):
    return api("GET", "info/" + module_id)


def wait_for_module(module_id, statuses, timeout):
    deadline = time.time() + timeout
    while True:
        info = module_info(module_id)
        if info.get("status") in statuses or time.time() > deadline:
            return info
        time.sleep(3)


def wallet_urls(module_id):
    deadline = time.time() + 60
    while True:
        urls = api("GET", "runner/browser/" + module_id).get("urls") or []
        if urls or time.time() > deadline:
            return urls
        time.sleep(2)


# --- wallet over adb ---------------------------------------------------------------------------

def adb(*args, timeout=60):
    return subprocess.run(["adb", *args], capture_output=True, timeout=timeout, check=False).stdout


NODE = re.compile(r"<node [^>]*>")


def attr(node, name):
    match = re.search(name + r'="([^"]*)"', node)
    return match.group(1) if match else ""


def screen():
    """The visible UI as a list of (text, content-desc, class, (x, y) centre)."""
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


def find(nodes, label):
    for text, desc, _, centre in nodes:
        if label in (text, desc):
            return centre
    return None


def tap(centre):
    adb("shell", "input", "tap", str(centre[0]), str(centre[1]))


def type_text(value):
    adb("shell", "input", "text", value)


def evidence(label):
    os.makedirs(ARTIFACTS, exist_ok=True)
    with open(os.path.join(ARTIFACTS, label + ".png"), "wb") as f:
        f.write(adb("exec-out", "screencap", "-p"))
    with open(os.path.join(ARTIFACTS, label + ".txt"), "w") as f:
        f.write("\n".join(texts(screen())) + "\n")


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


def onboard():
    """Set the PIN on a freshly installed wallet; a no-op if it is already set up."""
    adb("shell", "monkey", "-p", WALLET_PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    deadline = time.time() + 180
    while time.time() < deadline:
        nodes = screen()
        if dismiss_system_dialogs(nodes):
            continue
        shown = texts(nodes)
        if "Type a PIN" in shown or "Confirm PIN" in shown:
            type_text(PIN)
            time.sleep(3)
        elif "EU Digital Identity Wallet" in shown and find(nodes, "Continue"):
            tap(find(nodes, "Continue"))
            time.sleep(3)
        elif find(nodes, "GO TO HOME"):
            tap(find(nodes, "GO TO HOME"))
            time.sleep(3)
        elif "Welcome back" in shown:
            type_text(PIN)
            time.sleep(3)
        elif "Welcome" in shown or "Documents" in shown or "Home" in shown:
            print("wallet onboarded")
            return
        else:
            time.sleep(3)
    evidence("onboard-timeout")
    raise SystemExit("wallet onboarding did not reach the home screen within 180 s")


def accept_offer(offer_url, label):
    """Open a credential offer in the wallet and accept it. Returns 'issued' or the error text shown."""
    adb("shell", "am start -a android.intent.action.VIEW -d '" + offer_url + "'")
    deadline = time.time() + 180
    accepted = False
    while time.time() < deadline:
        nodes = screen()
        if dismiss_system_dialogs(nodes):
            continue
        shown = texts(nodes)
        if "Welcome back" in shown:
            type_text(PIN)
        elif "Document issued" in shown:
            evidence(label + "-issued")
            tap(find(nodes, "Close"))
            return "issued"
        elif "Oups! Something went wrong" in shown:
            evidence(label + "-error")
            message = " / ".join(t for t in shown if t != "Oups! Something went wrong")
            close = find(nodes, "Close")
            if close:
                tap(close)
            return "wallet error: " + message
        elif not accepted and "Issuance request" in shown and find(nodes, "Accept"):
            evidence(label + "-offer")
            tap(find(nodes, "Accept"))
            accepted = True
        time.sleep(2)
    evidence(label + "-timeout")
    return "wallet did not finish within 180 s"


# --- test runs ---------------------------------------------------------------------------------

def vci_issuance(config_path):
    alias = "android-ci-" + os.environ.get("CI_JOB_ID", str(int(time.time())))
    config = json.loads(open(config_path).read().replace("{ALIAS}", alias))
    plan = api("GET", "plan/" + create_plan(VCI_HAIP_PLAN, VCI_PLAN_VARIANT, config)["id"])
    print("plan", SERVER + "plan-detail.html?plan=" + plan["_id"])
    entry = next(m for m in plan["modules"]
                 if m["testModule"] == "oid4vci-1_0-wallet-test-credential-issuance"
                 and m["variant"].get("vci_credential_encryption") == "encrypted"
                 and m["variant"].get("vci_credential_issuance_mode") == "immediate")
    module_id = start_module(plan["_id"], entry["testModule"], entry["variant"])
    print("module", SERVER + "log-detail.html?log=" + module_id)
    urls = wallet_urls(module_id)
    if not urls:
        raise SystemExit("the module offered no credential offer URL")
    wallet_result = accept_offer(urls[0], "vci-issuance")
    info = wait_for_module(module_id, ("FINISHED", "INTERRUPTED"), 90)
    print("wallet:", wallet_result)
    print("module:", info.get("status"), info.get("result"))
    return info.get("status") == "FINISHED" and info.get("result") in ("PASSED", "WARNING")


def main():
    if len(sys.argv) != 3 or sys.argv[1] != "vci-issuance":
        raise SystemExit(__doc__)
    onboard()
    ok = vci_issuance(sys.argv[2])
    with open(os.path.join(ARTIFACTS, "logcat.txt"), "wb") as f:
        f.write(adb("logcat", "-d", timeout=120))
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
