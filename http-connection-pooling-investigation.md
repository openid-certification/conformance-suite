# HTTP Connection Pooling — Investigation Notes

Branch: `http-connection-pooling`. This document records what the branch does, the
root cause of the `#1466` "Path does not chain" failures that twice got connection
reuse reverted, and an honest assessment of how much (little) wall-clock time pooling
actually saves.

## TL;DR

- Pooling is now **correct**: the two real bugs that broke it before are fixed (a pool
  eviction that punched out in-use connections, and TLS session resumption silently
  dropping the client cert on mTLS connections).
- `#1466` is **root-caused end-to-end and reproduced deterministically**. It is an
  **Authlete-side (edge/proxy) behaviour, not a bug in this suite**: on a *resumed* TLS
  session the client cert is not re-presented, and Authlete's DCR cert-chain validation
  then has no client cert → "Path does not chain with any of the trust anchors".
- The **measured speedup is negligible** (~117 ms of handshake saved on a *reused* call,
  but reuse is only ~5–30 % and noisy, capped by our own 4 s idle eviction, against jobs
  that are dominated by deliberate protocol sleeps and external-OP round-trips). So the
  remaining decision is **merge-vs-park on cost/benefit, not correctness**.

## What the branch does

`AbstractCondition.createHttpClient` historically built a **fresh `HttpClient` + fresh
`SSLContext` + single-use `BasicHttpClientConnectionManager` per call**, so every outbound
condition call did a full TLS — and for FAPI a full mTLS — handshake.

When `options.cache_external_metadata` is set (the same opt-in the metadata cache uses),
the branch instead serves the request from a **shared, per-TLS-identity
`PoolingHttpClientConnectionManager`** (`condition/client/PooledConnectionManagers`), so
connections to the same host with the same client cert are reused across calls. Key
correctness properties:

- **Keyed by full TLS identity** = `sha256(cert + key + ca)` + the TLS-version flag, not
  the leaf cert (the keying bug behind the original revert — see history below).
- **Partitioned by identity** so a connection opened with cert A can never serve cert B.
- **Never `sc.init(null, …)` silently** — an mTLS identity with no usable KeyManager throws.
- The raw-socket TLS cipher/version tests (`AbstractCheckInsecureCiphers`,
  `AbstractDisallowTLSVersion`, `EnsureTLS12OrLater`) use `setupSocket()`, not
  `createHttpClient`, so they are unaffected.

Two follow-up fixes on top of the initial implementation:

1. **Failure-cascade fix** (`ed1fe37f2f`): the cap/deadline eviction used to `close()` an
   arbitrary shared manager, which aborted *every* concurrent request on that cert. Now the
   cap path only reaps a manager with `leased == 0 && available == 0 && pending == 0`, and a
   background sweep reaps an identity only after it has been idle past
   `REAP_AFTER_IDLE_NANOS`. In-use managers are never closed.
2. **Session-resumption fix** (`3ba4aad2c5`, tightened in `3067c690c5`) — the `#1466` fix,
   described next.

## Root cause of #1466 (the thing that got pooling reverted twice)

### Symptom

Intermittent `invalid_client … "Client certificate validation failed: Path does not chain
with any of the trust anchors"` from Authlete's Brazil DCR `/api/register`, with the **same,
correct** client cert sometimes accepted (201) and sometimes rejected (401) within one run.
`authlete-fapi-brazil-dcr.json` is the only CI config with a unique cert *and* a CA chain,
so it is the one that surfaces this.

### Mechanism (confirmed)

When the pool opens a **new** connection (a pool miss after the previous one was idle-reaped),
the shared per-identity `SSLContext`'s session cache lets it **resume** a cached TLS session.
A resumed/abbreviated handshake **does not re-send the client certificate** — and Authlete's
DCR cert-chain validation then has no client cert to chain, so it returns "Path does not chain".

- It is **deterministic per connection**: every *resumed* connection fails the cert check,
  every *full* handshake passes. The CI "intermittency" is simply the varying fraction of
  registrations that happen to land on a resumed vs a fresh connection.
- It is **Authlete-side and TLS-stack-independent** — reproduced with both the suite's own
  hc5/JSSE path *and* a raw OpenSSL client (see evidence). It is **not** a JSSE bug and **not**
  a bug in this suite; per TLS 1.2 a resumed session should inherit the peer cert, and
  Authlete's edge does not surface it to the application's cert validation.

### Reproduction & evidence

A throwaway harness drove the suite's **own** `createRestTemplate` + `PooledConnectionManagers`
(hc5 pooling, shared `SSLContext`) against the real `https://fapidev-30-as.authlete.net/api/register`
with the **exact failing identity** (`mtls:f564cca5…` — confirmed to be the Brazil transport cert
from the config, hashed in DER form; the PEM-form hash is `759ca9e6…`, same cert). A parallel
raw-OpenSSL probe isolated the TLS stack.

Critical gotcha that defeated earlier probes: the cert-chain check is **only reached with a real
registration body** (a `software_statement`). A trivial `{}` body bails at `400 "missing
software_statement"` *before* the cert check, so it never reproduces — which earlier produced a
false "Authlete honors resumed certs" conclusion. Using a real body extracted from a CI log
(even an expired one — the expiry check runs *after* the cert check) gives the exact CI split:

| Client / config | full handshake (cert presented) | resumed connection (cert dropped) |
|---|---|---|
| suite hc5/JSSE, resumption **on** | reaches `software_statement` check | **10–15 `path-does-not-chain`** per pattern |
| raw OpenSSL, real body | 16/16 pass (→ 400 expired) | **9/9 `path-does-not-chain`** |
| suite hc5/JSSE, resumption **off** (the fix) | all present cert | **0 `path-does-not-chain`** |

So: cert-present connections get past the cert check (and only then fail on the stale
`software_statement`); cert-dropped (resumed) connections fail the cert check — exactly the
201/401 pattern seen in CI.

### Where the cert is dropped (hypothesis, not yet confirmed)

Authlete terminates TLS at its **edge** (a GCP load balancer, and very likely an in-cluster
**Apache/mod_ssl** proxy tier in front of the Jetty app — the app's `Server: Jetty(9.4.56)`
header is passed straight through). The most probable precise mechanism is the **well-known
mod_ssl behaviour that on a resumed session the `SSL_CLIENT_*` variables are not re-populated**,
so the proxy forwards an empty client-cert header to the backend → "Path does not chain". This
matches the deterministic-per-connection result. Caveats before treating it as proven:

- The cluster config we have describes `*.authlete.com`; our target is
  `fapidev-30-as.authlete.**net**` (the FAPI *dev* instance) — possibly a different front.
- The ingress template is `extensions/v1beta1` (pre-2021); a default GCP L7 LB with only a
  server cert does not request client certs, yet our probe saw a `CertificateRequest` — so mTLS
  is terminated **deeper** than that LB (the Apache tier is the suspect). Confirming this would
  let us file a precise upstream report, but it does not change our fix.

### The fix

Disable TLS **session resumption** on the pooled mTLS `SSLContext` so every new connection does
a **full** handshake (re-presenting the cert), while ordinary TCP connection reuse is unaffected
(a reused connection presented its cert at the original handshake). Implementation
(`3067c690c5`, scoped to `PooledConnectionManagers.isEnabled(env) && km != null`):

- invalidate the SSL session **after** each handshake (`HandshakeCompletedListener`), and
- clear the client session context **before** each handshake (covers TLS 1.3 tickets);
- non-mTLS pooling keeps resumption (it has no cert to drop), non-pooled is unaffected.

Validated: buggy = 10–15 `path-does-not-chain` per pattern in both stacks; fixed = **0**.
Two full CI pipelines on the fix were green (Brazil DCR 15×201 / 0×401), with diagnostics
confirming **0** mTLS sessions resumed across connections and the only remaining
"path does not chain" being the `bad-mtls` test that *expects* it.

## Why pooling is not a meaningful speedup

The reuse benefit is real per call but small in aggregate and unreliable, and it lands on jobs
whose wall-clock is dominated by waiting, not handshakes.

1. **Per-reused-call saving ≈ 117 ms.** Measured Authlete call latency: fresh (full mTLS
   handshake) median **218 ms**, reused (handshake skipped) median **101 ms**.
2. **Reuse rate is low and noisy: ~5–30 %.** Across runs at the *same* config it measured 4 %,
   28 %, 31 %, and ~6 % — it is not a stable property; it depends on whether back-to-back calls
   happen to overlap within the keep-alive/eviction window, which is concurrency-timing noise.
   So the average saving across *all* calls is on the order of **~7 ms/call**, not 117 ms.
3. **Our own 4 s idle eviction caps the reuse window.** `IDLE_EVICT_SECONDS = 4` (chosen to sit
   under Authlete's advertised `Keep-Alive: timeout=5` to avoid stale reuse) closes idle
   connections after 4 s, so reuse only happens for calls within ~5 s of each other — fine for
   Authlete, but it discards connections that longer-keep-alive OPs would have reused.
4. **No multiplexing is possible.** The suite uses the **classic blocking** hc5 client (one
   request per connection), and Authlete (Jetty 9.4.56) **negotiates HTTP/1.1 only** — it
   refuses HTTP/2 (verified: ALPN `h2` offered, server picks `http/1.1`; forcing TLS 1.3 is
   rejected outright). So a busy connection cannot serve a second request; concurrent requests
   each need their own connection regardless of the pool.
5. **The jobs are wait-bound, not handshake-bound.** A module's conditions run sequentially, so
   its event-log span is its critical path; that span is dominated by **deliberate protocol
   sleeps** (CIBA poll waits, PAR/auth-code/expiry timers) and **external-OP round-trips**, with
   the server measured **56–68 % idle**. Saving ~0.1 s of handshake per module against
   seconds-to-minutes of waiting is within run-to-run CI variance (±minutes).

Net: handshake reuse shaves a per-call cost that is real but immaterial to the suite's wall
clock, and the headline reuse % is itself noisy. There is **no cleanly attributable wall-clock
speedup** from pooling in CI.

## History (why this was hard)

- **MR !1551** (2025-01) shipped pooling; **MR !1573** (2025-02) reverted it over `#1466`.
  That code never actually pooled mTLS *connections* — it pooled the non-mTLS client and cached
  `KeyManager[]` keyed only on the leaf cert string, a cache bug. The genuine cause of `#1466`
  (TLS session resumption, above) was never identified at the time.
- The 2026-06 retry on this branch first failed for two *different* reasons that are now fixed:
  the over-broad `cache_external_metadata` gate auto-enabling pooling across ~91 private configs,
  and the eviction cascade that closed in-use managers (`ed1fe37f2f`). With those fixed, the
  remaining failures were finally isolated to TLS session resumption.

## Status & recommendation

Pooling is **correct now** — both real bugs are fixed and `#1466` is understood and reproduced.
The open question is **value**: the measured speedup is negligible and the reuse rate is noisy,
so this is a cost/benefit call (added machinery: the pool registry, idle reaping, and the
resumption-disable, all to maintain) rather than a correctness one.

Independent of the merge decision, the diagnostics (`POOL-DIAG` logging in
`ConnectionReuseLoggingExec` / `PooledConnectionManagers`) are temporary and should be stripped
before any merge.
