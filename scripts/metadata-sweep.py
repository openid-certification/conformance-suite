#!/usr/bin/env python3
#
# TEMPORARY tool for the `vci-metadata-stricter-checks` branch.
#
# Runs the suite's headless discovery/metadata-only test modules against a list of
# real external servers (collected from prod+demo Mongo TEST_INFO) and reports EVERY
# condition that produced FAILURE/WARNING (the server-metadata schema checks added on this
# branch are flagged so they're easy to find in the full list). The goal is to catch false
# positives in the new stricter metadata checks before merging — running
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

# Conditions of particular interest, keyed by sweep category. The sweep reports EVERY
# condition that produced FAILURE/WARNING; the ones listed here are additionally flagged
# ("new" in the CSV / "[new]" in the summary) and have their SUCCESS count tracked, so the
# server-metadata schema checks added on this branch are easy to pick out of the full list.
NEW_CONDITIONS = {
    "discovery": [
        # server-metadata schema checks added on this branch (the same conditions run in
        # every protocol's discovery-endpoint verification and in federation validation).
        "ValidateServerMetadataAgainstSchema",
        "CheckForUnexpectedParametersInServerMetadata",
        "CheckDiscEndpointScopesSupportedSyntax",
    ],
    "vci": [
        # the server-metadata schema checks also run in the VCI issuer-metadata test via
        # the VCIDiscoveryEndpointChecks sequence.
        "ValidateServerMetadataAgainstSchema",
        "CheckForUnexpectedParametersInServerMetadata",
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
    rec = {"server": url, "test_id": None, "log_url": None, "overall_result": None,
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
    rec["log_url"] = "{}log-detail.html?log={}".format(conf.api_url_base, mid)
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
    rec["full_log"] = log

    flagged = set(NEW_CONDITIONS[category])
    for entry in log:
        src = entry.get("src")
        result = entry.get("result")
        is_new = src in flagged
        # Report every FAILURE/WARNING from any condition; additionally keep SUCCESS for the
        # flagged conditions so we can report their pass counts.
        if result in ("FAILURE", "WARNING") or (is_new and result == "SUCCESS"):
            rec["rows"].append({
                "condition": src,
                "result": result,
                "msg": entry.get("msg", ""),
                "requirements": entry.get("requirements"),
                "is_new": is_new,
            })

    results.append(rec)
    nfail = sum(1 for r in rec["rows"] if r["result"] == "FAILURE")
    nwarn = sum(1 for r in rec["rows"] if r["result"] == "WARNING")
    nnew = sum(1 for r in rec["rows"] if r["is_new"] and r["result"] in ("FAILURE", "WARNING"))
    suffix = " ERR={}".format(rec["error"]) if rec["error"] else ""
    print("[{}] {} -> {}/{} FAIL={} WARN={} (flagged findings={}){}  log={}".format(
        category, url, rec["overall_status"], rec["overall_result"], nfail, nwarn, nnew, suffix,
        rec["log_url"] or "(no test created)"))


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
        w.writerow(["server", "test_id", "condition", "result", "new", "msg",
                    "requirements", "overall_result", "overall_status", "error", "log_url"])
        for rec in results:
            base_tail = [rec["overall_result"], rec["overall_status"], rec["error"] or "",
                         rec["log_url"] or ""]
            if rec["rows"]:
                for r in rec["rows"]:
                    reqs = json.dumps(r["requirements"]) if r["requirements"] else ""
                    w.writerow([rec["server"], rec["test_id"], r["condition"], r["result"],
                                "yes" if r["is_new"] else "", r["msg"], reqs] + base_tail)
            else:
                w.writerow([rec["server"], rec["test_id"], "", "", "", "", ""] + base_tail)

    # Summary is built over every condition that actually fired (not a fixed list), so all
    # failures are reported; flagged conditions are marked with "new": true.
    flagged = set(NEW_CONDITIONS[category])
    summary = {}
    errors = []
    for rec in results:
        if rec["error"]:
            errors.append({"server": rec["server"], "error": rec["error"]})
        for r in rec["rows"]:
            cond = r["condition"] or "(no src)"
            bucket = summary.setdefault(
                cond, {"new": cond in flagged, "FAILURE": [], "WARNING": [], "SUCCESS_count": 0})
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

    log_urls = {rec["server"]: rec["log_url"] for rec in results}
    print("\n=== {} summary ({} servers, {} with findings) ===".format(
        category, len(results), findings))
    # Flagged conditions first, then by descending FAILURE+WARNING count.
    ordered = sorted(summary.items(),
                     key=lambda kv: (not kv[1]["new"],
                                     -(len(kv[1]["FAILURE"]) + len(kv[1]["WARNING"])), kv[0]))
    any_fired = False
    for cond, d in ordered:
        if d["FAILURE"] or d["WARNING"]:
            any_fired = True
            tag = " [new]" if d["new"] else ""
            print("  {}{}: FAILURE={} WARNING={} SUCCESS={}".format(
                cond, tag, len(d["FAILURE"]), len(d["WARNING"]), d["SUCCESS_count"]))
            for s in d["FAILURE"]:
                print("      FAIL  {}  ->  {}".format(s, log_urls.get(s) or "(no log)"))
            for s in d["WARNING"]:
                print("      WARN  {}  ->  {}".format(s, log_urls.get(s) or "(no log)"))
    if not any_fired:
        print("  no condition produced FAILURE/WARNING")

    # Detailed per-server findings: print every FAILURE/WARNING condition + full message inline,
    # so the failures are preserved in the CI job log after the review app (and the log_url links
    # that point at it) is torn down.
    print("\n--- {} detailed findings per server ---".format(category))
    printed = False
    for rec in results:
        findings = [r for r in rec["rows"] if r["result"] in ("FAILURE", "WARNING")]
        if not findings and not rec["error"]:
            continue
        printed = True
        print("  {}  (log: {})".format(rec["server"], rec["log_url"] or "no test created"))
        if rec["error"]:
            print("      ERROR    {}".format(rec["error"]))
        for r in sorted(findings, key=lambda x: x["result"]):
            tag = "[new] " if r.get("is_new") else ""
            msg = " ".join((r["msg"] or "").split())
            print("      {:8} {}{} : {}".format(r["result"], tag, r["condition"], msg))
    if not printed:
        print("  (no findings)")

    # Persist each test module's full result log to per-server files, so the complete failure
    # detail (the actual unknown property names and every condition's data) is preserved in the
    # GitLab artifacts after the review app — and the log_url links into it — are torn down.
    logs_dir = "{}-logs/{}".format(prefix, category)
    os.makedirs(logs_dir, exist_ok=True)
    for i, rec in enumerate(results):
        if not rec.get("full_log") and not rec["error"]:
            continue
        safe = "".join(c if c.isalnum() else "_" for c in rec["server"])[:80]
        with open("{}/{:03d}-{}.json".format(logs_dir, i, safe), "w") as f:
            json.dump({"server": rec["server"], "test_id": rec["test_id"],
                       "log_url": rec["log_url"], "overall_result": rec["overall_result"],
                       "overall_status": rec["overall_status"], "error": rec["error"],
                       "log": rec.get("full_log", [])}, f, indent=2)

    print("  ({} servers errored/unreachable)".format(len(errors)))
    print("  wrote {}, {} and per-server logs to {}/".format(rows_path, summ_path, logs_dir))


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
