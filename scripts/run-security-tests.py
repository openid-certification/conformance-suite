#!/usr/bin/env python3
"""
E2E security tests for the conformance suite's authentication and authorization.

Runs against a live server in non-dev mode (no DummyUserFilter) with API token
authentication. Tests cover:
- Share link (private link) access control for plan sharing
- Share link access control for test-level sharing
- Share-link JWT used directly as Authorization: Bearer on the API chain
- Unauthenticated access rejection
- Public access to published plans
- API token lifecycle

Usage:
    python3 scripts/run-security-tests.py

Environment variables:
    CONFORMANCE_SERVER  Base URL of the server (default: https://localhost.emobix.co.uk:8443/)
    CONFORMANCE_TOKEN   API token for admin access (required)
"""

import io
import json
import os
import sys
import time
import traceback
import urllib.parse
import zipfile

import httpx

# Add parent dir to path so we can import conformance.py
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def get_config():
    """Get server URL and token from environment."""
    base_url = os.environ.get("CONFORMANCE_SERVER", "https://localhost.emobix.co.uk:8443/")
    if not base_url.endswith("/"):
        base_url += "/"
    token = os.environ.get("CONFORMANCE_TOKEN")
    verify_ssl = os.environ.get("CONFORMANCE_SSL_VERIFY", "false").lower() != "false"
    return base_url, token, verify_ssl


def wait_for_server(base_url, token, verify_ssl, timeout=120):
    """Wait for the server to become ready."""
    print(f"  Waiting for server at {base_url} ...")
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            resp = httpx.get(f"{base_url}api/currentuser", verify=verify_ssl,
                             timeout=5, headers=headers)
            if resp.status_code < 500:
                print(f"  Server ready (HTTP {resp.status_code})")
                return
        except (httpx.ConnectError, httpx.ReadTimeout, httpx.ConnectTimeout):
            pass
        time.sleep(2)
    raise Exception(f"Server at {base_url} did not become ready within {timeout}s")


def create_test_plan(admin_client, base_url, plan_name, config_json, variant=None):
    """Create a test plan and return the plan ID and first module name."""
    api_url = f"{base_url}api/plan"
    params = {"planName": plan_name}
    if variant is not None:
        params["variant"] = json.dumps(variant)
    response = admin_client.post(api_url, params=params, content=config_json,
                                 headers={"Content-Type": "application/json"})
    if response.status_code != 201:
        raise Exception(f"Failed to create plan: HTTP {response.status_code} {response.text[:300]}")
    body = response.json()
    plan_id = body["id"]
    modules = body.get("modules", [])
    first_module_name = modules[0]["testModule"] if modules else None
    return plan_id, first_module_name


def create_test_from_plan(admin_client, base_url, plan_id, module_name):
    """Create a test instance from a plan module, return the test ID."""
    api_url = f"{base_url}api/runner"
    params = {"test": module_name, "plan": plan_id}
    response = admin_client.post(api_url, params=params)
    if response.status_code != 201:
        raise Exception(f"Failed to create test: HTTP {response.status_code} {response.text[:300]}")
    body = response.json()
    return body["id"]


def _extract_share_token(response):
    """Pull the JWT from a /share response and sanity-check it matches the `link`."""
    body = response.json()
    token = body["token"]
    link_token = urllib.parse.parse_qs(urllib.parse.urlparse(body["link"]).query)["token"][0]
    if token != link_token:
        raise Exception("Share response inconsistency: token field does not match link query parameter")
    return token


def generate_plan_share_link(admin_client, base_url, plan_id, exp_days="1"):
    """Generate a share link for a plan, return the JWT token."""
    api_url = f"{base_url}api/plan/{plan_id}/share"
    response = admin_client.post(api_url, params={"exp": exp_days})
    if response.status_code != 200:
        raise Exception(f"Failed to generate plan share link: HTTP {response.status_code} {response.text[:300]}")
    return _extract_share_token(response)


def generate_test_share_link(admin_client, base_url, test_id, exp_days="1"):
    """Generate a share link for a test, return the JWT token."""
    api_url = f"{base_url}api/info/{test_id}/share"
    response = admin_client.post(api_url, params={"exp": exp_days})
    if response.status_code != 200:
        raise Exception(f"Failed to generate test share link: HTTP {response.status_code} {response.text[:300]}")
    return _extract_share_token(response)


def bearer_client(base_url, jwt_token, verify_ssl):
    """Return an httpx.Client that sends the share JWT as Authorization: Bearer.

    Stateless: no cookies, no session. The server's API filter chain must recognise
    the JWT purely from the Authorization header via ShareJwtBearerAuthenticationProvider.
    """
    return httpx.Client(
        verify=verify_ssl,
        timeout=20,
        headers={"Authorization": f"Bearer {jwt_token}"})


def assert_valid_export_zip(runner, name, response, expected_test_ids):
    """Validate an export response: HTTP 200, application/zip Content-Type,
    body is a valid zip with intact CRCs, and contains a test-log-*.json for
    each expected test id."""
    runner.check_status(f"{name}: HTTP 200", response, 200)
    if response.status_code != 200:
        return
    content_type = response.headers.get("Content-Type", "")
    runner.check(f"{name}: Content-Type is application/zip",
                 content_type.startswith("application/zip"),
                 f"got Content-Type={content_type!r}")
    try:
        zf = zipfile.ZipFile(io.BytesIO(response.content))
    except zipfile.BadZipFile as e:
        runner.check(f"{name}: response body is a valid zip", False, str(e))
        return
    runner.check(f"{name}: response body is a valid zip", True)
    bad_entry = zf.testzip()
    runner.check(f"{name}: zip CRCs valid", bad_entry is None,
                 f"first bad entry: {bad_entry}")
    names = zf.namelist()
    for test_id in expected_test_ids:
        matches = [n for n in names if n.endswith(f"-{test_id}.json")]
        runner.check(f"{name}: zip contains test-log JSON for {test_id}",
                     len(matches) > 0, f"namelist sample: {names[:10]}")


def authenticate_private_link(base_url, jwt_token, verify_ssl):
    """
    Authenticate as a private link user via the OTT login flow.

    POST /login/ott with the token, follow the redirect to establish a session.
    Returns an httpx.Client with the authenticated session cookie.
    """
    client = httpx.Client(verify=verify_ssl, follow_redirects=False, timeout=20)

    ott_url = f"{base_url}login/ott"
    response = client.post(ott_url, data={"token": jwt_token})

    if response.status_code not in (302, 303):
        raise Exception(
            f"OTT login failed: expected redirect, got HTTP {response.status_code}. "
            f"Body: {response.text[:300]}")

    redirect_url = response.headers.get("Location", "")
    if redirect_url:
        if redirect_url.startswith("/"):
            redirect_url = base_url.rstrip("/") + redirect_url
        client.get(redirect_url)

    return client


class TestRunner:
    """Runs security tests and collects results."""

    def __init__(self):
        self.results = []
        self.failures = 0

    def check(self, name, condition, detail=""):
        if condition:
            self.results.append(("PASS", name, detail))
            print(f"  PASS: {name}")
        else:
            self.results.append(("FAIL", name, detail))
            self.failures += 1
            print(f"  FAIL: {name} -- {detail}")

    def check_status(self, name, response, expected_status):
        actual = response.status_code
        self.check(name, actual == expected_status,
                   f"expected HTTP {expected_status}, got {actual}")

    def check_status_in(self, name, response, expected_statuses):
        actual = response.status_code
        self.check(name, actual in expected_statuses,
                   f"expected HTTP status in {expected_statuses}, got {actual}")

    def summary(self):
        total = len(self.results)
        passed = total - self.failures
        print(f"\n{'=' * 60}")
        print(f"Security tests: {passed}/{total} passed, {self.failures} failed")
        if self.failures > 0:
            print("\nFailed tests:")
            for status, name, detail in self.results:
                if status == "FAIL":
                    print(f"  - {name}: {detail}")
        print(f"{'=' * 60}")
        return 1 if self.failures else 0


def run_tests():
    base_url, token, verify_ssl = get_config()
    print(f"Server: {base_url}")
    print(f"SSL verify: {verify_ssl}")

    if not token:
        print("ERROR: CONFORMANCE_TOKEN not set. Required for security tests.")
        print("Run via: ./scripts/run-integration-tests.sh --security-tests")
        return 1

    runner = TestRunner()

    wait_for_server(base_url, token, verify_ssl)

    # --- Precondition: verify server is NOT in dev mode ---
    # In dev mode, DummyUserFilter auto-authenticates all requests, which would
    # make unauthenticated rejection tests pass incorrectly (200 instead of 401).
    print("\n--- Precondition: verifying server is not in dev mode ---")
    probe = httpx.Client(verify=verify_ssl, timeout=10)
    probe_resp = probe.get(f"{base_url}api/currentuser")
    probe.close()
    if probe_resp.status_code == 200:
        print("  FATAL: Unauthenticated request returned 200.")
        print("  The server appears to be running in dev mode (DummyUserFilter active).")
        print("  Security tests require --fintechlabs.devmode=false.")
        return 1
    print(f"  OK: unauthenticated request returned HTTP {probe_resp.status_code}")

    # --- Setup phase: create test data as admin ---
    print("\n--- Setup: Creating test data as admin ---")

    admin_client = httpx.Client(verify=verify_ssl, timeout=20)
    admin_client.headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}",
    }

    # Verify admin token works
    resp = admin_client.get(f"{base_url}api/currentuser")
    if resp.status_code != 200:
        print(f"ERROR: Admin token rejected (HTTP {resp.status_code}). Check CONFORMANCE_TOKEN.")
        admin_client.close()
        return 1
    admin_info = resp.json()
    print(f"  Admin user: {admin_info.get('displayName', 'unknown')}")

    # Minimal config for the OIDC config test plan
    config = json.dumps({
        "description": "security-test",
        "server": {
            "discoveryUrl": "https://example.com/.well-known/openid-configuration"
        }
    })
    plan_name = "oidcc-config-certification-test-plan"

    plan_id, first_module = create_test_plan(admin_client, base_url, plan_name, config)
    print(f"  Created plan: {plan_id} (module: {first_module})")

    test_id = create_test_from_plan(admin_client, base_url, plan_id, first_module)
    print(f"  Created test: {test_id}")

    # Publish the plan (needed for public access tests)
    publish_resp = admin_client.post(
        f"{base_url}api/plan/{plan_id}/publish",
        content=json.dumps({"publish": "everything"}),
        headers={"Content-Type": "application/json"})
    if publish_resp.status_code != 200:
        raise Exception(f"Failed to publish plan: HTTP {publish_resp.status_code} {publish_resp.text[:300]}")
    print(f"  Published plan: {plan_id}")

    # Create a second (unpublished) plan for "cannot access other plans" tests
    other_plan_id, other_module = create_test_plan(admin_client, base_url, plan_name, config)
    other_test_id = create_test_from_plan(admin_client, base_url, other_plan_id, other_module)
    print(f"  Created other plan: {other_plan_id} (test: {other_test_id})")

    # Generate plan-level share link
    plan_jwt = generate_plan_share_link(admin_client, base_url, plan_id)
    print(f"  Generated plan share link (length: {len(plan_jwt)})")

    # Generate test-level share link
    test_jwt = generate_test_share_link(admin_client, base_url, test_id)
    print(f"  Generated test share link (length: {len(test_jwt)})")

    # ===================================================================
    # 1. PLAN SHARING TESTS
    # ===================================================================
    print("\n--- 1. Plan sharing: authenticating as private link user ---")
    pl_client = authenticate_private_link(base_url, plan_jwt, verify_ssl)
    print("  Authenticated successfully")

    # Precondition check
    print("\n--- 1. Plan sharing: precondition check ---")
    resp = pl_client.get(f"{base_url}api/currentuser")
    if resp.status_code == 200:
        user_info = resp.json()
        is_guest = user_info.get("isGuest", False)
        is_admin = user_info.get("isAdmin", True)
        runner.check("Plan share: user is guest", is_guest,
                     f"isGuest={is_guest}")
        runner.check("Plan share: user is not admin", not is_admin,
                     f"isAdmin={is_admin}")
        if not is_guest or is_admin:
            print("\n  FATAL: Private link user has wrong identity. Skipping.")
            pl_client.close()
            admin_client.close()
            return runner.summary()
    else:
        runner.check("Plan share: can access currentuser", False,
                     f"HTTP {resp.status_code}")
        print("  FATAL: Cannot verify identity. Skipping.")
        pl_client.close()
        admin_client.close()
        return runner.summary()

    # Allowed access
    print("\n--- 1. Plan sharing: allowed access ---")
    resp = pl_client.get(f"{base_url}api/plan/{plan_id}")
    runner.check_status("Plan share: can view shared plan", resp, 200)

    resp = pl_client.get(f"{base_url}api/info/{test_id}")
    runner.check_status("Plan share: can view test info", resp, 200)

    resp = pl_client.get(f"{base_url}api/log/{test_id}")
    runner.check_status("Plan share: can view test log", resp, 200)

    resp = pl_client.get(f"{base_url}plan-detail.html?plan={plan_id}")
    runner.check_status("Plan share: can view plan-detail page", resp, 200)

    resp = pl_client.get(f"{base_url}log-detail.html?log={test_id}")
    runner.check_status("Plan share: can view log-detail page", resp, 200)

    # The export endpoints are NOT in the private-link allowlist
    # (WebSecurityResourceServerConfig.java:75-83 only allows /api/plan/{id},
    # /api/info/{id}, /api/log/{id} as single-segment URIs). Private-link
    # users therefore get 403 from the security layer regardless of which plan
    # they target — even the one their share covers.
    resp = pl_client.get(f"{base_url}api/plan/exporthtml/{plan_id}")
    runner.check_status("Plan share: cannot export shared plan (not in allowlist)", resp, 403)

    resp = pl_client.get(f"{base_url}api/log/exporthtml/{test_id}")
    runner.check_status("Plan share: cannot export shared test html (not in allowlist)", resp, 403)

    resp = pl_client.get(f"{base_url}api/log/export/{test_id}")
    runner.check_status("Plan share: cannot export shared test zip (not in allowlist)", resp, 403)

    # Denied access
    print("\n--- 1. Plan sharing: denied access ---")
    resp = pl_client.get(f"{base_url}api/plan/{other_plan_id}")
    runner.check_status("Plan share: cannot view other plan", resp, 404)

    resp = pl_client.get(f"{base_url}api/plan/exporthtml/{other_plan_id}")
    runner.check_status("Plan share: cannot export other plan", resp, 403)

    resp = pl_client.get(f"{base_url}api/log/exporthtml/{other_test_id}")
    runner.check_status("Plan share: cannot export other test html", resp, 403)

    resp = pl_client.get(f"{base_url}api/log/export/{other_test_id}")
    runner.check_status("Plan share: cannot export other test zip", resp, 403)

    resp = pl_client.post(f"{base_url}api/info/{test_id}/publish",
                          content=json.dumps({"publish": "summary"}),
                          headers={"Content-Type": "application/json"})
    runner.check_status("Plan share: cannot publish test", resp, 403)

    resp = pl_client.post(f"{base_url}api/plan/{plan_id}/share", params={"exp": "1"})
    runner.check_status("Plan share: cannot create share link", resp, 403)

    resp = pl_client.post(f"{base_url}api/info/{test_id}/share", params={"exp": "1"})
    runner.check_status("Plan share: cannot share test", resp, 403)

    resp = pl_client.post(f"{base_url}api/plan/{plan_id}/publish")
    runner.check_status("Plan share: cannot publish", resp, 403)

    resp = pl_client.post(f"{base_url}api/plan/{plan_id}/makemutable")
    runner.check_status("Plan share: cannot make mutable", resp, 403)

    resp = pl_client.delete(f"{base_url}api/plan/{plan_id}")
    runner.check_status("Plan share: cannot delete", resp, 403)

    resp = pl_client.post(f"{base_url}api/plan",
                          params={"planName": plan_name},
                          content=config,
                          headers={"Content-Type": "application/json"})
    runner.check_status("Plan share: cannot create new plan", resp, 403)

    resp = pl_client.get(f"{base_url}plan-detail.html?plan={other_plan_id}")
    runner.check_status_in("Plan share: cannot view other plan page", resp, {403, 302})

    # Token API denied for private link users
    resp = pl_client.get(f"{base_url}api/token")
    runner.check_status("Plan share: cannot list tokens", resp, 403)

    # Invalid tokens
    print("\n--- 1. Plan sharing: invalid tokens ---")
    tampered = list(plan_jwt)
    last_dot = plan_jwt.rfind(".")
    tampered[last_dot + 1] = "B" if tampered[last_dot + 1] == "A" else "A"

    bad_client = httpx.Client(verify=verify_ssl, follow_redirects=False, timeout=20)
    resp = bad_client.post(f"{base_url}login/ott", data={"token": "".join(tampered)})
    runner.check("Tampered token rejected",
                 resp.status_code != 302 or "error" in resp.headers.get("Location", ""),
                 f"HTTP {resp.status_code}, Location: {resp.headers.get('Location', 'none')}")
    bad_client.close()

    garbage_client = httpx.Client(verify=verify_ssl, follow_redirects=False, timeout=20)
    resp = garbage_client.post(f"{base_url}login/ott", data={"token": "not-a-jwt"})
    runner.check("Garbage token rejected",
                 resp.status_code != 302 or "error" in resp.headers.get("Location", ""),
                 f"HTTP {resp.status_code}, Location: {resp.headers.get('Location', 'none')}")
    garbage_client.close()

    pl_client.close()

    # ===================================================================
    # 2. TEST-LEVEL SHARING TESTS
    # ===================================================================
    print("\n--- 2. Test sharing: authenticating ---")
    tl_client = authenticate_private_link(base_url, test_jwt, verify_ssl)
    print("  Authenticated successfully")

    resp = tl_client.get(f"{base_url}api/currentuser")
    if resp.status_code == 200:
        tl_info = resp.json()
        runner.check("Test share: user is guest", tl_info.get("isGuest", False),
                     f"isGuest={tl_info.get('isGuest')}")

    print("\n--- 2. Test sharing: access control ---")
    resp = tl_client.get(f"{base_url}api/info/{test_id}")
    runner.check_status("Test share: can view shared test info", resp, 200)

    resp = tl_client.get(f"{base_url}api/log/{test_id}")
    runner.check_status("Test share: can view shared test log", resp, 200)

    resp = tl_client.get(f"{base_url}log-detail.html?log={test_id}")
    runner.check_status("Test share: can view log-detail page", resp, 200)

    resp = tl_client.get(f"{base_url}api/plan/{plan_id}")
    runner.check_status("Test share: can view plan containing test", resp, 200)

    resp = tl_client.get(f"{base_url}api/plan/{other_plan_id}")
    runner.check_status("Test share: cannot view other plan", resp, 404)

    resp = tl_client.get(f"{base_url}api/info/{other_test_id}")
    runner.check_status("Test share: cannot view other test", resp, 404)

    # Same allowlist applies to test-level shares: export endpoints not allowed.
    resp = tl_client.get(f"{base_url}api/log/exporthtml/{test_id}")
    runner.check_status("Test share: cannot export shared test html (not in allowlist)", resp, 403)

    resp = tl_client.get(f"{base_url}api/log/export/{test_id}")
    runner.check_status("Test share: cannot export shared test zip (not in allowlist)", resp, 403)

    resp = tl_client.get(f"{base_url}api/log/exporthtml/{other_test_id}")
    runner.check_status("Test share: cannot export other test html", resp, 403)

    resp = tl_client.get(f"{base_url}api/log/export/{other_test_id}")
    runner.check_status("Test share: cannot export other test zip", resp, 403)

    resp = tl_client.post(f"{base_url}api/info/{test_id}/publish",
                          content=json.dumps({"publish": "summary"}),
                          headers={"Content-Type": "application/json"})
    runner.check_status("Test share: cannot publish test", resp, 403)

    tl_client.close()

    # ===================================================================
    # 2b. SHARE-LINK JWT AS API BEARER TOKEN (plan-level)
    # ===================================================================
    # Same allow/deny matrix as the session flow above, but the JWT is sent
    # directly as Authorization: Bearer on the /api/* filter chain — no
    # /login/ott dance, no session cookie.
    print("\n--- 2b. Plan JWT as Bearer: precondition check ---")
    plan_bearer = bearer_client(base_url, plan_jwt, verify_ssl)

    resp = plan_bearer.get(f"{base_url}api/currentuser")
    runner.check_status("Plan JWT Bearer: currentuser reachable", resp, 200)
    if resp.status_code == 200:
        info = resp.json()
        runner.check("Plan JWT Bearer: user is guest",
                     info.get("isGuest", False),
                     f"isGuest={info.get('isGuest')}")
        runner.check("Plan JWT Bearer: user is not admin",
                     not info.get("isAdmin", True),
                     f"isAdmin={info.get('isAdmin')}")

    print("\n--- 2b. Plan JWT as Bearer: allowed access ---")
    resp = plan_bearer.get(f"{base_url}api/plan/{plan_id}")
    runner.check_status("Plan JWT Bearer: can view shared plan", resp, 200)

    resp = plan_bearer.get(f"{base_url}api/info/{test_id}")
    runner.check_status("Plan JWT Bearer: can view test info", resp, 200)

    resp = plan_bearer.get(f"{base_url}api/log/{test_id}")
    runner.check_status("Plan JWT Bearer: can view test log", resp, 200)

    print("\n--- 2b. Plan JWT as Bearer: denied access ---")
    resp = plan_bearer.get(f"{base_url}api/plan/{other_plan_id}")
    runner.check_status("Plan JWT Bearer: cannot view other plan", resp, 404)

    resp = plan_bearer.post(f"{base_url}api/plan/{plan_id}/share", params={"exp": "1"})
    runner.check_status("Plan JWT Bearer: cannot create share link", resp, 403)

    resp = plan_bearer.post(f"{base_url}api/info/{test_id}/share", params={"exp": "1"})
    runner.check_status("Plan JWT Bearer: cannot share test", resp, 403)

    resp = plan_bearer.post(f"{base_url}api/plan/{plan_id}/publish")
    runner.check_status("Plan JWT Bearer: cannot publish", resp, 403)

    resp = plan_bearer.delete(f"{base_url}api/plan/{plan_id}")
    runner.check_status("Plan JWT Bearer: cannot delete plan", resp, 403)

    resp = plan_bearer.post(f"{base_url}api/plan",
                            params={"planName": plan_name},
                            content=config,
                            headers={"Content-Type": "application/json"})
    runner.check_status("Plan JWT Bearer: cannot create new plan", resp, 403)

    resp = plan_bearer.get(f"{base_url}api/token")
    runner.check_status("Plan JWT Bearer: cannot list tokens", resp, 403)

    # Collection endpoints are not in the private-link allow-list
    resp = plan_bearer.get(f"{base_url}api/log")
    runner.check_status_in("Plan JWT Bearer: cannot list all logs", resp, {401, 403})

    # Other /api/* endpoints not in the private-link allow-list
    resp = plan_bearer.get(f"{base_url}api/runner/available")
    runner.check_status_in("Plan JWT Bearer: cannot hit runner", resp, {401, 403})

    resp = plan_bearer.get(f"{base_url}api/server")
    runner.check_status_in("Plan JWT Bearer: cannot hit api/server", resp, {401, 403})

    # HTML pages are served by the non-API filter chain which has no BearerTokenAuthenticationFilter.
    # The Bearer JWT therefore cannot reach log-detail.html / plan-detail.html — those require a
    # /login/ott-established session. Expect redirect to login or 401/403.
    plan_bearer_no_redirect = httpx.Client(
        verify=verify_ssl, timeout=20, follow_redirects=False,
        headers={"Authorization": f"Bearer {plan_jwt}"})
    resp = plan_bearer_no_redirect.get(f"{base_url}plan-detail.html?plan={plan_id}")
    runner.check_status_in("Plan JWT Bearer: cannot access plan-detail.html",
                           resp, {302, 401, 403})
    resp = plan_bearer_no_redirect.get(f"{base_url}log-detail.html?log={test_id}")
    runner.check_status_in("Plan JWT Bearer: cannot access log-detail.html",
                           resp, {302, 401, 403})
    plan_bearer_no_redirect.close()

    plan_bearer.close()

    # ===================================================================
    # 2c. SHARE-LINK JWT AS API BEARER TOKEN (test-level)
    # ===================================================================
    print("\n--- 2c. Test JWT as Bearer: access control ---")
    test_bearer = bearer_client(base_url, test_jwt, verify_ssl)

    resp = test_bearer.get(f"{base_url}api/currentuser")
    runner.check_status("Test JWT Bearer: currentuser reachable", resp, 200)
    if resp.status_code == 200:
        runner.check("Test JWT Bearer: user is guest",
                     resp.json().get("isGuest", False),
                     f"isGuest={resp.json().get('isGuest')}")

    resp = test_bearer.get(f"{base_url}api/info/{test_id}")
    runner.check_status("Test JWT Bearer: can view shared test info", resp, 200)

    resp = test_bearer.get(f"{base_url}api/log/{test_id}")
    runner.check_status("Test JWT Bearer: can view shared test log", resp, 200)

    resp = test_bearer.get(f"{base_url}api/plan/{plan_id}")
    runner.check_status("Test JWT Bearer: can view containing plan", resp, 200)

    resp = test_bearer.get(f"{base_url}api/info/{other_test_id}")
    runner.check_status("Test JWT Bearer: cannot view other test", resp, 404)

    resp = test_bearer.get(f"{base_url}api/plan/{other_plan_id}")
    runner.check_status("Test JWT Bearer: cannot view other plan", resp, 404)

    test_bearer.close()

    # ===================================================================
    # 2d. INVALID BEARER JWTs
    # ===================================================================
    print("\n--- 2d. Invalid Bearer JWTs ---")

    tampered_jwt = list(plan_jwt)
    last_dot = plan_jwt.rfind(".")
    tampered_jwt[last_dot + 1] = "B" if tampered_jwt[last_dot + 1] == "A" else "A"
    tamper_client = bearer_client(base_url, "".join(tampered_jwt), verify_ssl)
    resp = tamper_client.get(f"{base_url}api/plan/{plan_id}")
    runner.check_status("Bearer JWT: tampered signature rejected", resp, 401)
    tamper_client.close()

    garbage_client = bearer_client(base_url, "not-a-jwt", verify_ssl)
    resp = garbage_client.get(f"{base_url}api/plan/{plan_id}")
    runner.check_status("Bearer JWT: malformed token rejected", resp, 401)
    garbage_client.close()

    # Regression: the admin API token used throughout these tests is presented
    # as Authorization: Bearer and continues to work — this validates the
    # two-provider chain order in WebSecurityResourceServerConfig.
    api_token_bearer = bearer_client(base_url, token, verify_ssl)
    resp = api_token_bearer.get(f"{base_url}api/currentuser")
    runner.check_status("Bearer regression: opaque API token still authenticates", resp, 200)
    api_token_bearer.close()

    # ===================================================================
    # 3. UNAUTHENTICATED ACCESS REJECTION
    # ===================================================================
    print("\n--- 3. Unauthenticated access rejection ---")
    noauth_client = httpx.Client(verify=verify_ssl, timeout=20)

    resp = noauth_client.get(f"{base_url}api/plan")
    runner.check_status("Unauth: plan list rejected", resp, 401)

    resp = noauth_client.get(f"{base_url}api/plan/{plan_id}")
    runner.check_status("Unauth: plan detail rejected", resp, 401)

    resp = noauth_client.get(f"{base_url}api/info/{test_id}")
    runner.check_status("Unauth: test info rejected", resp, 401)

    resp = noauth_client.get(f"{base_url}api/log/{test_id}")
    runner.check_status("Unauth: test log rejected", resp, 401)

    resp = noauth_client.get(f"{base_url}api/runner/available")
    runner.check_status("Unauth: runner rejected", resp, 401)

    resp = noauth_client.post(f"{base_url}api/plan",
                              params={"planName": plan_name},
                              content=config,
                              headers={"Content-Type": "application/json"})
    runner.check_status("Unauth: create plan rejected", resp, 401)

    resp = noauth_client.get(f"{base_url}api/currentuser")
    runner.check_status("Unauth: currentuser rejected", resp, 401)

    resp = noauth_client.get(f"{base_url}api/token")
    runner.check_status("Unauth: token list rejected", resp, 401)

    noauth_client.close()

    # ===================================================================
    # 4. PUBLIC ACCESS
    # ===================================================================
    print("\n--- 4. Public access ---")
    pub_client = httpx.Client(verify=verify_ssl, timeout=20)

    resp = pub_client.get(f"{base_url}api/plan/{plan_id}?public=true")
    runner.check_status("Public: published plan visible", resp, 200)

    resp = pub_client.get(f"{base_url}api/log/{test_id}?public=true")
    runner.check_status("Public: published test log visible", resp, 200)

    resp = pub_client.get(f"{base_url}api/plan/{other_plan_id}?public=true")
    runner.check_status("Public: unpublished plan not visible", resp, 404)

    resp = pub_client.get(f"{base_url}plan-detail.html?plan={plan_id}&public=true")
    runner.check_status("Public: published plan-detail page accessible", resp, 200)

    # Only the JSON-only export endpoints (/api/log/export/?* and
    # /api/plan/export/?*) are in the public matcher. The html-zip variants
    # /api/{plan,log}/exporthtml/?* require auth regardless of ?public=true,
    # by design — the UI hides their download buttons in public-mode views so
    # this 401 should never be reached from the rendered pages.
    resp = pub_client.get(f"{base_url}api/plan/exporthtml/{plan_id}?public=true")
    runner.check_status("Public: plan/exporthtml needs auth even with ?public", resp, 401)

    resp = pub_client.get(f"{base_url}api/log/exporthtml/{test_id}?public=true")
    runner.check_status("Public: log/exporthtml needs auth even with ?public", resp, 401)

    resp = pub_client.get(f"{base_url}api/log/export/{test_id}?public=true")
    assert_valid_export_zip(runner, "Public: can export published test zip", resp, [test_id])

    resp = pub_client.get(f"{base_url}api/log/export/{other_test_id}?public=true")
    runner.check_status("Public: cannot export unpublished test zip", resp, 404)

    # Without ?public=true, published plan still requires auth
    resp = pub_client.get(f"{base_url}api/plan/{plan_id}")
    runner.check_status("Public: plan without ?public requires auth", resp, 401)

    resp = pub_client.get(f"{base_url}api/plan/exporthtml/{plan_id}")
    runner.check_status("Public: export without ?public requires auth", resp, 401)

    resp = pub_client.get(f"{base_url}api/log/exporthtml/{test_id}")
    runner.check_status("Public: log/exporthtml without ?public requires auth", resp, 401)

    resp = pub_client.get(f"{base_url}api/log/export/{test_id}")
    runner.check_status("Public: log/export without ?public requires auth", resp, 401)

    resp = pub_client.post(f"{base_url}api/info/{test_id}/publish",
                           content=json.dumps({"publish": "summary"}),
                           headers={"Content-Type": "application/json"})
    runner.check_status("Public: unauth cannot publish test", resp, 401)

    pub_client.close()

    # ===================================================================
    # 4b. PLAN EXPORT — owner path
    # ===================================================================
    # Smoke test for the bulk-load path in LogApi.exportPlanAsZip when the
    # caller owns the plan (the path every CI test job exercises). Asserts
    # the response is a well-formed zip containing the expected test logs.
    print("\n--- 4b. Plan export (owner) ---")
    resp = admin_client.get(f"{base_url}api/plan/exporthtml/{plan_id}")
    assert_valid_export_zip(runner, "Export: owner can export own plan", resp, [test_id])

    resp = admin_client.get(f"{base_url}api/plan/exporthtml/does-not-exist")
    runner.check_status("Export: missing plan id returns 404", resp, 404)

    resp = admin_client.get(f"{base_url}api/log/exporthtml/{test_id}")
    assert_valid_export_zip(runner, "Export: owner can export own test html", resp, [test_id])

    resp = admin_client.get(f"{base_url}api/log/export/{test_id}")
    assert_valid_export_zip(runner, "Export: owner can export own test zip", resp, [test_id])

    resp = admin_client.get(f"{base_url}api/log/exporthtml/does-not-exist")
    runner.check_status("Export: missing test id html returns 404", resp, 404)

    resp = admin_client.get(f"{base_url}api/log/export/does-not-exist")
    runner.check_status("Export: missing test id zip returns 404", resp, 404)

    # ===================================================================
    # 4c. PER-TEST PUBLISH (/api/info/{id}/publish)
    # ===================================================================
    # other_test_id was created against an unpublished plan, so it starts
    # with publish=null. Admin can change its publish level via this
    # endpoint without going through /plan/{id}/publish.
    print("\n--- 4c. Per-test publish ---")
    resp = admin_client.post(f"{base_url}api/info/{other_test_id}/publish",
                             content=json.dumps({"publish": "summary"}),
                             headers={"Content-Type": "application/json"})
    runner.check_status("Publish: admin can publish test as summary", resp, 200)

    if resp.status_code == 200:
        # Confirm the change is observable to public callers.
        check_client = httpx.Client(verify=verify_ssl, timeout=20)
        resp = check_client.get(f"{base_url}api/info/{other_test_id}?public=true")
        runner.check_status("Publish: published test now visible publicly", resp, 200)
        check_client.close()

    resp = admin_client.post(f"{base_url}api/info/{other_test_id}/publish",
                             content=json.dumps({"publish": "everything"}),
                             headers={"Content-Type": "application/json"})
    runner.check_status("Publish: admin can raise level to everything", resp, 200)

    resp = admin_client.post(f"{base_url}api/info/{other_test_id}/publish",
                             content=json.dumps({"publish": "garbage-value"}),
                             headers={"Content-Type": "application/json"})
    runner.check_status_in("Publish: invalid publish value rejected", resp, {400, 403})

    resp = admin_client.post(f"{base_url}api/info/{other_test_id}/publish",
                             content=json.dumps({}),
                             headers={"Content-Type": "application/json"})
    runner.check_status("Publish: missing publish field rejected", resp, 400)

    # ===================================================================
    # 5. API TOKEN LIFECYCLE
    # ===================================================================
    print("\n--- 5. API token lifecycle ---")

    resp = admin_client.get(f"{base_url}api/token")
    runner.check_status("Token: admin can list tokens", resp, 200)
    if resp.status_code == 200:
        token_list = resp.json()
        runner.check("Token: list contains the auth token", len(token_list) > 0,
                     f"got {len(token_list)} tokens")

    admin_client.close()

    return runner.summary()


def main():
    try:
        exit_code = run_tests()
    except Exception:
        traceback.print_exc()
        print("\nSecurity tests failed with an exception")
        exit_code = 1
    sys.exit(exit_code)


if __name__ == "__main__":
    main()
