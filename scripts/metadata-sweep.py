#!/usr/bin/env python3
#
# TEMPORARY tool for the `vci-metadata-stricter-checks` branch.
#
# Runs the suite's headless discovery/metadata-only test modules against a list of
# real external servers (collected from prod+demo Mongo TEST_INFO) and reports which
# of the NEW conditions added on this branch produced FAILURE/WARNING. The goal is to
# catch false positives in the new stricter metadata checks before merging — running
# from the suite's own network so reachability matches the deployed environment, and
# letting the suite compute the correct .well-known location and parse signed metadata.
#
# This is informational only — real servers legitimately violate some rules, so the
# exit code is always 0. Triage the output by hand (see the plan / README of the MR).
#
# Revert this script (and the .gitlab-ci/metadata-sweep-*.txt lists and the metadata_sweep
# CI job) before merging the branch.
#
# Usage:
#   CONFORMANCE_SERVER=https://localhost.emobix.co.uk:8443/ \
#     python3 scripts/metadata-sweep.py \
#       --discovery-urls .gitlab-ci/metadata-sweep-discovery.txt \
#       --vci-urls .gitlab-ci/metadata-sweep-vci.txt \
#       --out-prefix metadata-sweep
#
# Server/token/dev-mode resolution mirrors run-test-plan.py: CONFORMANCE_SERVER (defaults
# to the local dev URL), CONFORMANCE_TOKEN when not in dev mode, CONFORMANCE_DEV_MODE /
# DISABLE_SSL_VERIFY env flags.

import argparse
import asyncio
import csv
import json
import os
import sys

from conformance import Conformance, ServerUnavailableError, UnrecoverableHTTPError

# Conditions ADDED on the vci-metadata-stricter-checks branch, keyed by sweep category.
# We filter event-log entries by their `src` (the condition's simple class name).
NEW_CONDITIONS = {
    "discovery": [
        # The only new non-VCI/federation check; same condition runs in every protocol's
        # discovery-endpoint verification and in federation metadata validation.
        "CheckDiscEndpointScopesSupportedSyntax",
    ],
    "vci": [
        "VCIValidateCredentialIssuerUri",
        "VCIValidateAuthorizationServersAreHttps",
        "VCIValidateCredentialSigningAlgValuesSupported",
        "VCIValidateProofSigningAlgValuesSupported",
        "VCIValidateProofTypesCoPresence",
        "VCIValidateDisplayLocales",
        "VCIWarnOnNonCanonicalDisplayLocales",
        "VCIValidateCredentialConfigurationScopeSyntax",
        "VCIValidateEncryptionAlgorithms",
        "VCIValidateEncryptionZipValues",
        "VCIValidateAuthorizationServerLocalesSyntax",
        "VCIWarnOnNonCanonicalAuthorizationServerLocales",
        "CheckDiscEndpointScopesSupportedSyntax",
        "VCIEnsureHttpsUrlsMetadata",
        "VCIEnsureAuthorizationServerIssuerMatchesExpected",
    ],
}

# Test module + variant selection per category. The variant only affects which
# non-target checks run; we filter to the new conditions, so its exact value doesn't
# matter beyond being a legal combination that lets the module instantiate.
TEST = {
    "discovery": {
        "name": "fapi2-security-profile-final-discovery-end-point-verification",
        "variant": {
            "client_auth_type": "private_key_jwt",
            "authorization_request_type": "simple",
            "fapi_profile": "plain_fapi",
            "sender_constrain": "mtls",
            "fapi_request_method": "unsigned",
            "fapi_response_mode": "plain_response",
            "openid": "plain_oauth",
        },
    },
    "vci": {
        "name": "oid4vci-1_0-issuer-metadata-test",
        # vci (not vci_haip): every new check runs in the common path; vci_haip adds only
        # pre-existing HAIP conditions, so it gives no extra new-code coverage.
        "variant": {"client_auth_type": "private_key_jwt", "fapi_profile": "vci"},
    },
}


def build_config(category, url):
    if category == "discovery":
        if "/.well-known/" not in url:
            url = url.rstrip("/") + "/.well-known/openid-configuration"
        return json.dumps({"server": {"discoveryUrl": url}})
    # vci: pass the issuer as-is; the suite computes the correct well-known location.
    return json.dumps({"vci": {"credential_issuer_url": url}})


def read_urls(path):
    urls = []
    with open(path) as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#"):
                urls.append(line)
    return urls


async def run_one(conf, category, url, results, per_server_timeout):
    test = TEST[category]
    rec = {"server": url, "test_id": None, "overall_result": None,
           "overall_status": None, "rows": [], "error": None}
    try:
        info = await conf.create_test_with_variant(test["name"], build_config(category, url), test["variant"])
        rec["test_id"] = info.get("id")
    except Exception as e:
        rec["error"] = "create_test: {}".format(e)
        results.append(rec)
        print("[{}] {} -> create failed: {}".format(category, url, e))
        return

    mid = rec["test_id"]
    try:
        # autoStart is true for these modules, so creation already started the test.
        await conf.wait_for_state(mid, ["FINISHED"], timeout=per_server_timeout)
    except Exception as e:
        # INTERRUPTED or our own timeout — record it but still read the (partial) log,
        # which usually still contains the metadata-check condition results.
        rec["error"] = "wait: {}".format(e)

    try:
        mi = await conf.get_module_info(mid)
        rec["overall_result"] = mi.get("result")
        rec["overall_status"] = mi.get("status")
    except Exception as e:
        rec["error"] = (rec["error"] + "; " if rec["error"] else "") + "info: {}".format(e)

    try:
        log = await conf.get_test_log(mid)
    except Exception as e:
        rec["error"] = (rec["error"] + "; " if rec["error"] else "") + "log: {}".format(e)
        log = []

    wanted = set(NEW_CONDITIONS[category])
    for entry in log:
        if entry.get("src") in wanted:
            rec["rows"].append({
                "condition": entry.get("src"),
                "result": entry.get("result"),
                "msg": entry.get("msg", ""),
                "requirements": entry.get("requirements"),
            })

    results.append(rec)
    nfail = sum(1 for r in rec["rows"] if r["result"] == "FAILURE")
    nwarn = sum(1 for r in rec["rows"] if r["result"] == "WARNING")
    suffix = " ERR={}".format(rec["error"]) if rec["error"] else ""
    print("[{}] {} -> {}/{} new-cond FAIL={} WARN={}{}".format(
        category, url, rec["overall_status"], rec["overall_result"], nfail, nwarn, suffix))


async def run_category(conf, category, urls, concurrency, per_server_timeout):
    results = []
    q = asyncio.Queue()
    for u in urls:
        q.put_nowait(u)

    async def worker():
        while True:
            try:
                u = q.get_nowait()
            except asyncio.QueueEmpty:
                return
            try:
                await run_one(conf, category, u, results, per_server_timeout)
            except Exception as e:
                # Never let one server kill the sweep.
                print("[{}] {} -> worker exception (continuing): {}".format(category, u, e))
            finally:
                q.task_done()

    workers = [asyncio.create_task(worker()) for _ in range(concurrency)]
    await asyncio.gather(*workers, return_exceptions=True)
    return results


def write_outputs(category, results, prefix):
    rows_path = "{}-{}-rows.csv".format(prefix, category)
    summ_path = "{}-{}-summary.json".format(prefix, category)

    with open(rows_path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["server", "test_id", "condition", "result", "msg",
                    "requirements", "overall_result", "overall_status", "error"])
        for rec in results:
            base_tail = [rec["overall_result"], rec["overall_status"], rec["error"] or ""]
            if rec["rows"]:
                for r in rec["rows"]:
                    reqs = json.dumps(r["requirements"]) if r["requirements"] else ""
                    w.writerow([rec["server"], rec["test_id"], r["condition"], r["result"],
                                r["msg"], reqs] + base_tail)
            else:
                w.writerow([rec["server"], rec["test_id"], "", "", "", ""] + base_tail)

    summary = {c: {"FAILURE": [], "WARNING": [], "SUCCESS_count": 0}
               for c in sorted(set(NEW_CONDITIONS[category]))}
    errors = []
    for rec in results:
        if rec["error"]:
            errors.append({"server": rec["server"], "error": rec["error"]})
        for r in rec["rows"]:
            bucket = summary[r["condition"]]
            if r["result"] == "FAILURE":
                bucket["FAILURE"].append(rec["server"])
            elif r["result"] == "WARNING":
                bucket["WARNING"].append(rec["server"])
            elif r["result"] == "SUCCESS":
                bucket["SUCCESS_count"] += 1

    findings = sum(1 for rec in results
                   if any(r["result"] in ("FAILURE", "WARNING") for r in rec["rows"]))
    out = {
        "category": category,
        "servers_total": len(results),
        "servers_with_findings": findings,
        "conditions": summary,
        "errors": errors,
    }
    with open(summ_path, "w") as f:
        json.dump(out, f, indent=2)

    print("\n=== {} summary ({} servers, {} with findings) ===".format(
        category, len(results), findings))
    any_fired = False
    for cond, d in summary.items():
        if d["FAILURE"] or d["WARNING"]:
            any_fired = True
            print("  {}: FAILURE={} WARNING={} SUCCESS={}".format(
                cond, len(d["FAILURE"]), len(d["WARNING"]), d["SUCCESS_count"]))
            for s in d["FAILURE"]:
                print("      FAIL  {}".format(s))
            for s in d["WARNING"]:
                print("      WARN  {}".format(s))
    if not any_fired:
        print("  no new condition produced FAILURE/WARNING")
    print("  ({} servers errored/unreachable)".format(len(errors)))
    print("  wrote {} and {}".format(rows_path, summ_path))


def make_conformance():
    server = os.environ.get("CONFORMANCE_SERVER") or "https://localhost.emobix.co.uk:8443/"
    if not server.endswith("/"):
        server += "/"
    dev_mode = ("CONFORMANCE_DEV_MODE" in os.environ) or ("CONFORMANCE_SERVER" not in os.environ)
    token = None if dev_mode else os.environ.get("CONFORMANCE_TOKEN")
    verify_ssl = (not dev_mode) and ("DISABLE_SSL_VERIFY" not in os.environ)
    print("Using conformance server {} (dev_mode={}, verify_ssl={})".format(server, dev_mode, verify_ssl))
    return Conformance(server, token, verify_ssl)


async def main():
    parser = argparse.ArgumentParser(description="Sweep real servers' metadata through the suite's discovery/metadata tests")
    parser.add_argument("--discovery-urls", help="File of auth-server discovery URLs / issuers, one per line")
    parser.add_argument("--vci-urls", help="File of VCI credential_issuer_url values, one per line")
    parser.add_argument("--out-prefix", default="metadata-sweep", help="Output file prefix")
    parser.add_argument("--concurrency", type=int, default=4, help="Concurrent tests")
    parser.add_argument("--per-server-timeout", type=int,
                        default=int(os.getenv("SWEEP_PER_SERVER_TIMEOUT", "180")),
                        help="Seconds to wait for each test to finish")
    parser.add_argument("--limit", type=int, default=0, help="Only run the first N URLs per category (for testing)")
    args = parser.parse_args()

    if not args.discovery_urls and not args.vci_urls:
        parser.error("supply --discovery-urls and/or --vci-urls")

    # Cap RetryTransport's retry window: a create_test that 500s (e.g. an invalid
    # variant/config) is permanent, not transient — don't let it retry for the full
    # 360s default per server. Still leaves ample room for genuine transient blips.
    os.environ.setdefault("CONFORMANCE_RETRY_TIMEOUT", "60")

    conf = make_conformance()
    try:
        await conf.wait_for_server_ready()
    except ServerUnavailableError as e:
        print("failed to connect to conformance suite: {}".format(e))
        sys.exit(1)

    try:
        jobs = []
        if args.discovery_urls:
            jobs.append(("discovery", args.discovery_urls))
        if args.vci_urls:
            jobs.append(("vci", args.vci_urls))
        for category, path in jobs:
            urls = read_urls(path)
            if args.limit:
                urls = urls[:args.limit]
            print("\n### sweeping {} {} servers from {}".format(len(urls), category, path))
            results = await run_category(conf, category, urls, args.concurrency, args.per_server_timeout)
            write_outputs(category, results, args.out_prefix)
    finally:
        await conf.close_client()


if __name__ == "__main__":
    asyncio.run(main())
