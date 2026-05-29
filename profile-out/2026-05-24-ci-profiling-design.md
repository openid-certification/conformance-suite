# CI profiling — design

**Date:** 2026-05-24
**Author:** Joseph Heenan (drafted with Claude Code)
**Companion:** [local-profiling report](20260524-080708/report.md)

## Context

The local profile of a happy-path FAPI test ([report](20260524-080708/report.md))
found the JVM 96.7 % idle: locally the suite is wait-bound on cross-process
HTTP, with no hot method to optimise. CI is a different story — the
conformance suite is deployed as a single Spring Boot pod (GKE,
`auto-deploy`-style Helm release per branch), and every integration test job
in a pipeline hits that one JVM concurrently. CPU contention is the working
hypothesis for why CI runs take ~40 min, with the first ~10 min being the
specific window we'd most like to make faster.

We want a profile of the deployed JVM during a real CI pipeline so we can:
- Confirm or refute the CPU-contention hypothesis with actual data.
- Find the hot methods / threads under merged load (which we can't see locally because the JVM never runs under that load).
- Have a profile ready in-hand whenever we suspect something has slowed CI down.

## Goals

- **Opt-in per pipeline.** No baseline overhead on regular CI runs. Triggered by setting a CI variable, no commit / file changes required.
- **Fully automated within the pipeline.** No mid-pipeline manual play-button, no need for a person to time anything.
- **Covers the full pipeline window** (~40 min), with enough fidelity in the first 10 min to be useful for optimisation.
- **One artifact to download** at the end of the pipeline, openable in JDK Mission Control or convertible to a flamegraph.

## Non-goals

- **Per-test-job isolation.** Impossible with a shared JVM; the profile shows merged activity and you filter by thread name post-hoc.
- **Cross-pipeline aggregation / regression tracking.** No central profile store. Each profiled run produces its own artifact, compared by eye.
- **Production / staging / demo deploys.** Scope is review-branch deploys only (`deploy-review`). Other environments would each need their own decision about whether sharing a JFR file is acceptable.

## Design

### Opt-in mechanism

One CI variable: `CONFORMANCE_PROFILE`. Empty / unset → no profiling, no overhead, no harvest job. Set to anything truthy (e.g. `1`) → profiling enabled for that pipeline.

Set via the GitLab "Run pipeline" UI (Variables section), or as a top-level pipeline-trigger argument. No `.gitlab-ci.yml` rule needs to read the value, just its presence.

### JFR recording

The JVM is configured via `JAVA_TOOL_OPTIONS` (JVM-native env var) — no entrypoint change, and **deliberately not via `javaArgs` / `JAVA_EXTRA_ARGS`** so the chart's existing flags (`-Xmx724M`, `-Djdk.tls.maxHandshakeMessageSize=65536`, `-XX:+HeapDumpOnOutOfMemoryError`, `-XX:HeapDumpPath=/dump/`, see `chart/values.yaml:32`) are preserved. The JVM concatenates `JAVA_TOOL_OPTIONS` with command-line args at startup; the two are additive.

```
JAVA_TOOL_OPTIONS=
  -XX:FlightRecorderOptions=repository=/dump/jfr-repo-${PIPELINE_ID}
  -XX:StartFlightRecording=name=cipipe-${PIPELINE_ID},settings=profile,
    filename=/dump/cipipe-${PIPELINE_ID}.jfr,disk=true,dumponexit=true,maxsize=2g,
    jdk.InitialEnvironmentVariable#enabled=false,
    jdk.InitialSystemProperty#enabled=false,
    jdk.JVMInformation#enabled=false,
    jdk.SystemProcess#enabled=false,
    jdk.ProcessStart#enabled=false
```

(One line in actual use — broken here for readability. `${PIPELINE_ID}` is rendered by Helm from `.Values.profilePipelineId`; the deploy script passes `$CI_PIPELINE_ID` from GitLab.)

Why each setting:

- **`-XX:FlightRecorderOptions=repository=/dump/jfr-repo-${PIPELINE_ID}`** — places JFR's *active* on-disk repository on the persistent `/dump` PVC, in a pipeline-unique subdirectory. Default location is `$java.io.tmpdir/<random>` (on the overlay FS, not persistent). The recording's `filename=` only controls the *dump destination*; the active chunks live in the repository while the recording runs.
- **`name=cipipe-${PIPELINE_ID}`** — pipeline-unique handle the harvest job uses with `jcmd JFR.stop`. **A bare `name=cipipe` would be unsafe**: two profiled pipelines on the same review branch are not serialised by `resource_group: deploy` (.gitlab-ci.yml:265) past the deploy stage. If pipeline A's harvest runs against pipeline B's pod (B's helm upgrade rolled the deployment after A's deploy), `jcmd JFR.stop name=cipipe` would stop B's recording and write the dump to A's filename — A walks away with B's data labelled as A's artifact. Adding the pipeline ID to the name makes a cross-pipeline stop fail cleanly (no matching recording) instead of silently mis-attributing data.
- **`settings=profile`** — full method-sample + allocation + lock data. ~1–2 % JVM overhead. The lighter `settings=default` would shrink the file ~5–10× but loses allocation detail the local report already showed is meaningful (21 % of allocations were in event-log paths).
- **`filename=/dump/cipipe-${PIPELINE_ID}.jfr`** — dump destination, same PVC as repository, **pipeline-unique**. `/dump` is a PVC (chart/templates/deployment.yaml:131) shared across all pipelines on this review env: a fixed filename would let a prior pipeline's leftover `cipipe.jfr` get harvested as if it were a current-pipeline restart. The `${PIPELINE_ID}` suffix means the harvest job only ever sees files from its own pipeline.
- **`disk=true`** — JFR spools events to the repository on disk rather than RAM. Default in modern JVMs; stated for clarity.
- **`dumponexit=true`** — when the JVM **shuts down gracefully** (SIGTERM, system.exit), the recording is written to `filename=`. This covers k8s rolling-restart / scale-down. It does **not** cover SIGKILL / OOMKill, which bypass shutdown hooks; the harvest script's restart-detection (below) is what mitigates that case best-effort.
- **`maxsize=2g`** — **critical**: without this, JDK 21 silently applies a 250 MB default (verified — JVM logs `"No limit specified, using maxsize=250MB as default"`). Once the recording hits maxsize, JFR drops **the oldest** chunks first, which is exactly the data we most want (first ~10 min of the pipeline). 2 GB comfortably covers a 40-min `settings=profile` run with significant headroom on the 9.8 GB volume.
- **Five `<event>#enabled=false` overrides** — **security-critical redaction**. By default, `settings=profile` enables several events that capture command-line-shaped data:
  - `jdk.InitialEnvironmentVariable` — every env var the JVM received at startup. The pod's env (`chart/templates/deployment.yaml:43–98`) includes `SIGNING_KEY`, `DEPRECATED_SIGNING_KEY`, `PRIVATE_LINK_SIGNING_KEY`, `OIDC_GOOGLE_CLIENTID`, `OIDC_GOOGLE_SECRET`.
  - `jdk.InitialSystemProperty` — every `-D` arg the JVM received at startup.
  - `jdk.JVMInformation` — JVM build / args summary, includes the command line.
  - `jdk.SystemProcess` — scans `/proc` at endChunk and records **every process visible to the container, including the JVM itself, with its full `commandLine`**. Critical here: `Dockerfile:8` expands the secret env vars listed above into `-D` arguments on the JVM's own command line (e.g. `-Dfintechlabs.signingKey=${SIGNING_KEY}`, `-Doidc.google.secret=${OIDC_GOOGLE_SECRET}`). The local JFR captured during the earlier profiling run already contains a `jdk.SystemProcess` event for the suite JVM with its full Java invocation — verified by `jfr print --events jdk.SystemProcess profile-out/.../cpu.jfr`. Disabling `jdk.InitialEnvironmentVariable` alone does **not** close this hole.
  - `jdk.ProcessStart` — defence in depth; captures processes spawned by the JVM with their command lines (none expected in this app but cheap to disable).
  JFR per-event overrides on the command line (`<event>#<setting>=<value>`) disable each event before the recording ever captures it — no custom JFC file needed.

The JVM runs `/server/fapi-test-suite.jar` as PID 8 (verified). `jcmd` is in the image (verified).

### Deploy step change

Two changes, both narrow:

**1. Chart (`chart/templates/deployment.yaml`)** — append a conditional env entry to the existing `env:` block (where `BASE_URL`, `JAVA_EXTRA_ARGS`, `SIGNING_KEY` etc. are already defined, currently around line 43–98):

```yaml
{{- if .Values.profile }}
- name: JAVA_TOOL_OPTIONS
  value: >-
    -XX:FlightRecorderOptions=repository=/dump/jfr-repo-{{ .Values.profilePipelineId }}
    -XX:StartFlightRecording=name=cipipe-{{ .Values.profilePipelineId }},settings=profile,filename=/dump/cipipe-{{ .Values.profilePipelineId }}.jfr,disk=true,dumponexit=true,maxsize=2g,jdk.InitialEnvironmentVariable#enabled=false,jdk.InitialSystemProperty#enabled=false,jdk.JVMInformation#enabled=false,jdk.SystemProcess#enabled=false,jdk.ProcessStart#enabled=false
{{- end }}
```

`.Values.profile` defaults to unset → no env change → identical pod to today. `.Values.profilePipelineId` is the GitLab pipeline ID, passed in by the deploy script; embedding it in the filename and repo path means the harvest job can address files unambiguously and stale files from prior pipelines aren't mistaken for current-run output. The chart's existing `JAVA_EXTRA_ARGS` (carrying the `javaArgs` heap / TLS / heap-dump flags) is untouched; `JAVA_TOOL_OPTIONS` is JVM-native and additive.

**2. Deploy script (`.gitlab-ci.yml`, the `deploy` shell function at line 676)** — pass `--set profile=true` when the variable is set **and** we're on a review branch. The `deploy()` function is shared by `deploy-review`, `deploy-normal`, and `deploy-staging`, so the branch gate has to be inside the function (or we'd risk enabling profiling on master/production/demo pipelines, which is out of scope and would write JFR data into a pod whose harvest job won't run):

```bash
PROFILE_HELM=""
if [ -n "$CONFORMANCE_PROFILE" ] && [ -n "$CI_COMMIT_BRANCH" ] \
   && [ "$CI_COMMIT_BRANCH" != "master" ] \
   && [ "$CI_COMMIT_BRANCH" != "production" ] \
   && [ "$CI_COMMIT_BRANCH" != "demo" ]; then
  PROFILE_HELM="--set profile=true --set profilePipelineId=$CI_PIPELINE_ID"
fi
helm upgrade ... \
  --set application.env_slug="$CI_ENVIRONMENT_SLUG" \
  --set releaseOverride="$CI_ENVIRONMENT_SLUG" \
  --set application.track="$track" \
  $PROFILE_HELM
```

The branch predicate matches `deploy-review`'s rule (.gitlab-ci.yml:303) and the `collect_jfr` rule below. When the variable is unset, `$PROFILE_HELM` is empty and the deploy is byte-identical to today.

### Harvest job

New job in `.gitlab-ci.yml`:

```yaml
collect_jfr:
  stage: compare          # runs after the 'test' stage, before 'cleanup'
  extends: .auto-deploy   # provides kubectl context
  environment:
    name: review/$CI_COMMIT_REF_NAME
    action: access        # required so GitLab populates $CI_ENVIRONMENT_SLUG,
                          # which $KUBE_NAMESPACE depends on (.gitlab-ci.yml:263)
  rules:
    - if: $CONFORMANCE_PROFILE && $CI_COMMIT_BRANCH && $CI_COMMIT_BRANCH != "master" && $CI_COMMIT_BRANCH != "production" && $CI_COMMIT_BRANCH != "demo"
  when: always            # run even if upstream test jobs failed — a failing
                          # pipeline is among the runs we most want to inspect
  script:
    - auto-deploy use_kube_context || true
    - POD=$(kubectl -n "$KUBE_NAMESPACE" get pods -l app="$CI_ENVIRONMENT_SLUG",track=stable -o jsonpath='{.items[0].metadata.name}')
    - test -n "$POD" || { echo "no pod found"; exit 1; }
    # restartPolicy is Always (chart/templates/deployment.yaml:126), so if the JVM
    # crashed mid-pipeline the kubelet started a fresh container with a new "cipipe"
    # recording. Log the restart count so the artifact consumer can tell which
    # scenario produced the data.
    - RESTARTS=$(kubectl -n "$KUBE_NAMESPACE" get pod "$POD" -o jsonpath='{.status.containerStatuses[0].restartCount}')
    - echo "container restart count: $RESTARTS"
    # Prior dumponexit file from THIS pipeline (only present if the JVM
    # restarted during this pipeline AND shut down gracefully before its
    # replacement started). The pipeline-unique filename means we cannot pick
    # up stale files from previous pipelines that ran against the same PVC.
    - PRIOR=/dump/cipipe-${CI_PIPELINE_ID}.jfr
    - |
      if kubectl -n "$KUBE_NAMESPACE" exec "$POD" -- test -f "$PRIOR"; then
        echo "found prior dumponexit file from this pipeline"
        kubectl -n "$KUBE_NAMESPACE" cp "$POD:$PRIOR" cipipe-prior.jfr
      fi
    # Stop the recording on the current JVM and write the dump to a distinct
    # filename so it cannot collide with the prior file. JFR.stop (vs
    # JFR.dump) ends the recording, which means the JVM's later graceful
    # shutdown at cleanup-stage scale-down will NOT fire dumponexit again —
    # so we don't accumulate an orphaned file on the PVC.
    # The recording name is pipeline-unique: targeting cipipe-${CI_PIPELINE_ID}
    # makes a cross-pipeline stop fail cleanly instead of mis-attributing data.
    - CURRENT=/dump/cipipe-${CI_PIPELINE_ID}-current.jfr
    - STOPPED=false
    - |
      if kubectl -n "$KUBE_NAMESPACE" exec "$POD" -- jcmd 8 JFR.stop name="cipipe-${CI_PIPELINE_ID}" filename="$CURRENT"; then
        echo "live recording stopped and dumped"
        STOPPED=true
        kubectl -n "$KUBE_NAMESPACE" cp "$POD:$CURRENT" cipipe-current.jfr
      else
        echo "JFR.stop failed (JVM may not be running, or recording name not found — could be wrong pod from a racing pipeline)"
      fi
    - test -f cipipe-prior.jfr -o -f cipipe-current.jfr || { echo "no JFR data harvested"; exit 1; }
    - for f in cipipe-prior.jfr cipipe-current.jfr; do [ -f "$f" ] && gzip "$f"; done
    # Enforce upload budget so we don't lose the whole artifact to a failed
    # upload. GitLab.com per-job cap is 1 GB; we target 900 MB combined.
    # Single recording at maxsize=2g compresses to ~400-700 MB typically;
    # combined prior+current can exceed 1 GB compressed in pathological runs.
    - BUDGET_MB=900
    - WITHIN_BUDGET=true
    - total_mb() { du -cm "$@" 2>/dev/null | awk 'END{print $1+0}'; }
    - SIZE=$(total_mb cipipe-*.jfr.gz)
    - echo "harvested artifact total: ${SIZE} MB (budget ${BUDGET_MB} MB)"
    - |
      if [ "$SIZE" -gt "$BUDGET_MB" ] && [ -f cipipe-prior.jfr.gz ]; then
        echo "WARNING: combined size ${SIZE} MB exceeds budget; dropping cipipe-prior.jfr.gz to fit"
        rm cipipe-prior.jfr.gz
        SIZE=$(total_mb cipipe-current.jfr.gz)
      fi
    - |
      if [ "$SIZE" -gt "$BUDGET_MB" ]; then
        echo "ERROR: cipipe-current.jfr.gz alone is ${SIZE} MB, exceeds budget ${BUDGET_MB} MB."
        echo "Artifact upload may fail (1 GB GitLab cap). Pod-side cleanup skipped so the raw recording can be retrieved manually with:"
        echo "  kubectl -n $KUBE_NAMESPACE cp $POD:$CURRENT cipipe-current.jfr"
        WITHIN_BUDGET=false
      fi
    # Pod-side cleanup is gated on (a) confirmed stop and (b) within-budget.
    # If JFR.stop failed transiently while the JVM was still recording,
    # deleting the repo would destabilise the live recording. If we exceeded
    # the upload budget, leaving the pod-side files in place is the manual
    # recovery path. Files left behind in either case are bounded by the
    # pipeline-unique paths and addressed via the open-question item on PVC
    # orphan cleanup.
    - |
      if $STOPPED && $WITHIN_BUDGET; then
        kubectl -n "$KUBE_NAMESPACE" exec "$POD" -- sh -c "rm -f /dump/cipipe-${CI_PIPELINE_ID}*.jfr && rm -rf /dump/jfr-repo-${CI_PIPELINE_ID}" || true
      else
        echo "skipping pod-side cleanup (STOPPED=$STOPPED, WITHIN_BUDGET=$WITHIN_BUDGET)"
      fi
  artifacts:
    access: developer       # match other deployed-pod artifacts (.gitlab-ci.yml:45,293,405)
    when: always
    paths:
      - cipipe-current.jfr.gz
      - cipipe-prior.jfr.gz
    expire_in: 30 days
```

Key choices:

- **`stage: compare`** — runs strictly after all `test`-stage jobs (which is what we want for a "record everything the test jobs did" model). Runs before `cleanup`, so the pod still exists when we `kubectl exec`.
- **`environment: review/$CI_COMMIT_REF_NAME`** with `action: access` — GitLab only populates `$CI_ENVIRONMENT_SLUG` when a job declares an environment, and `$KUBE_NAMESPACE` (.gitlab-ci.yml:263) depends on it. Without this, the selector would resolve to an empty slug. Pattern mirrors `scale_down env` (.gitlab-ci.yml:341).
- **No `needs:` list** — stage ordering already enforces "after all test jobs". The job therefore waits for the entire `test` stage to drain, including `local_test`, `security_test`, `frontend_e2e_test`, and the unit `test:` job — none of which load the deployed JVM but the wait is acceptable in exchange for not having to maintain an explicit `needs: [{job: X, optional: true}, ...]` list of ~12 entries.
- **Selector `app=$CI_ENVIRONMENT_SLUG,track=stable`** — the chart sets the `app` label to `releaseOverride`, which the deploy script sets to `$CI_ENVIRONMENT_SLUG` (.gitlab-ci.yml:759). `track=stable` is the chart default (`application.track`).
- **`when: always`** — the harvest must run even if `fapi_test` failed. Without this, GitLab would skip the job because of the implicit "previous stages must succeed" gate, and we'd lose the profile of exactly the pipeline we'd most want.
- **`artifacts: access: developer`** — matches the existing convention for deployment / test artifacts (.gitlab-ci.yml:45, 293, 405). Even with the redaction event-overrides, a JFR file with method samples / allocation hotspots / lock contention from a deployed pod is sensitive operational data that shouldn't be publicly downloadable.
- **Restart-aware capture (best-effort in v1)** — the chart's `restartPolicy: Always` (chart/templates/deployment.yaml:126) means a crashed JVM is replaced by a fresh one with its own brand-new `cipipe-${PIPELINE_ID}` recording. The script captures both: (a) the prior JVM's dumponexit file at `/dump/cipipe-${CI_PIPELINE_ID}.jfr` if it exists, and (b) the current JVM's live dump at `/dump/cipipe-${CI_PIPELINE_ID}-current.jfr`. Several known limitations are accepted in v1:
  - **SIGKILL'd prior JVMs** (OOMKill, k8s eviction): `dumponexit` never runs, no prior file written. Partial data exists as raw repository chunks under `/dump/jfr-repo-${CI_PIPELINE_ID}/` but the script does not extract those (see Open questions — chunk recovery).
  - **Multiple graceful restarts within a single pipeline**: each new JVM's eventual `dumponexit` writes to the same `/dump/cipipe-${CI_PIPELINE_ID}.jfr` path, so only the **most recent** prior segment survives. Earlier segments from intermediate restarts are lost.
  - **Termination grace period vs dump time**: `terminationGracePeriodSeconds: 30` (chart/templates/deployment.yaml:130) is the wall-clock budget the JVM has between SIGTERM and SIGKILL. We have not measured how long `dumponexit` takes to flush a near-`maxsize=2g` recording to `/dump`. If the dump exceeds 30 s the kubelet sends SIGKILL and the dump is incomplete (or absent).
  All three failure modes degrade the prior-segment artifact but **do not** affect the live-dump artifact from the surviving JVM, which is what `JFR.stop` in the harvest produces.

The harvest uses `JFR.stop` (rather than `JFR.dump`) so the recording is fully ended before the file/repo are removed. The trade-off: any compare-stage activity hitting the pod after the harvest runs is not recorded — but the compare jobs in this pipeline only run `run_compare` against test artifacts and don't load the deployed JVM, so there's nothing of interest to capture in that window.

### Optional: render a flamegraph in CI

If `jfrconv` (from async-profiler) is added to the harvest job's image, a single extra line renders a self-contained HTML flamegraph alongside the raw file:

```yaml
    - test -f cipipe-current.jfr.gz && gunzip -k cipipe-current.jfr.gz && jfrconv --wall -o html cipipe-current.jfr cipipe-current.html
  artifacts:
    access: developer
    paths:
      - cipipe-current.jfr.gz
      - cipipe-prior.jfr.gz
      - cipipe-current.html
```

`--wall` because we want to see the merged activity including idle / I/O wait (vs `--cpu` which only shows on-CPU samples — useful but a subset). HTML is browser-openable directly from the GitLab artifacts UI.

This is optional. If we skip it, users download `cipipe-current.jfr.gz` (and `cipipe-prior.jfr.gz` if present), decompress locally, and open in JDK Mission Control. Adding it makes the loop faster but requires distributing async-profiler to the harvest job. Recommend deferring until after the first profiled run shows the raw-JFR loop is too slow.

## Files changed

1. **`chart/templates/deployment.yaml`** — append a `{{- if .Values.profile }}` block to the container's existing `env:` list (currently lines 43–98) that injects `JAVA_TOOL_OPTIONS` with the full JFR option string.
2. **`.gitlab-ci.yml` — the `deploy` shell function around line 750–765** — conditionally pass `--set profile=true` to `helm upgrade` when `$CONFORMANCE_PROFILE` is set.
3. **`.gitlab-ci.yml`** — add the new `collect_jfr` job (see Harvest job above).

That's it for the JFR data-path. No Dockerfile change (jcmd already present in the Temurin JDK base). No changes to test jobs, the `javaArgs` value, or the entrypoint script.

## Verification

Spec-level — already confirmed:

- [x] GitLab artifact size cap. GitLab.com instance default is 1 GB per job; the conformance-suite project inherits this with no lowering. Plenty of headroom for a 40-min `settings=profile` recording (~300 MB–1 GB raw, ~100–350 MB gzipped).
- [x] `jcmd` present in the deployed image (verified via `kubectl exec`).
- [x] `/dump` volume exists and is writable (9.8 GB, currently 24 KB used).
- [x] JFR per-event overrides work as command-line settings on JDK 21 (`<event>#enabled=false` syntax, verified locally).
- [x] JDK 21 silently applies `maxsize=250MB` when not specified (verified — JVM logs the warning).
- [x] `settings=profile` defaults enable the five secret-leaking events listed in JFR recording (verified in `/etc/java-21-openjdk/jfr/profile.jfc`; `jdk.SystemProcess.commandLine` capture confirmed via `jfr print` against the local profiling run's JFR file).

Acceptance — a profiled pipeline:

1. **Trigger** pipeline on a review branch with `CONFORMANCE_PROFILE=1`. The pipeline runs to completion in roughly the usual ~40 min (no major slowdown from profiling).
2. **Off-by-default sanity** — separately, trigger a pipeline **without** the variable and confirm:
   - `collect_jfr` does not appear in the pipeline graph.
   - The deployed pod has **no** `JAVA_TOOL_OPTIONS` env var (`kubectl exec ... env | grep JAVA_TOOL_OPTIONS` is empty).
3. **Harvest** — `collect_jfr` appears, `when: always` triggers it even if a test job failed, and at least `cipipe-current.jfr.gz` is downloadable from the pipeline artifacts UI. (`cipipe-prior.jfr.gz` will also be present if the pod restarted gracefully during the run; expected to be absent in a healthy run.)
4. **Loadable** — the file unzips and opens in JDK Mission Control with method samples / threads / events across the full pipeline window. The recording duration matches the test-stage duration (no first-10-min truncation from a hidden maxsize).
5. **Redaction sentinel** — before the profiled run, set a sentinel env var on the pod (e.g. add `CANARY_DO_NOT_LEAK=this-must-not-appear-in-jfr` to `chart/templates/deployment.yaml`'s env block for the test). After harvest, decompress both artifacts and run `jfr print` against each:
   ```
   gunzip -k cipipe-current.jfr.gz
   jfr print cipipe-current.jfr | grep -F CANARY_DO_NOT_LEAK
   [ -f cipipe-prior.jfr.gz ] && gunzip -k cipipe-prior.jfr.gz && jfr print cipipe-prior.jfr | grep -F CANARY_DO_NOT_LEAK
   ```
   Both `grep` invocations must produce no output. Repeat for a known `-D` sentinel passed via `javaArgs`. Use `jfr print` rather than `strings` so decoded event fields (including those that may not be plain UTF-8 in the binary) are searched. Remove the sentinel before merging.
6. **Off-by-default on master/production/demo** — set `CONFORMANCE_PROFILE=1` on a master pipeline and confirm the staging pod has **no** `JAVA_TOOL_OPTIONS` env var (the branch gate in `deploy()` should suppress it).

## Open questions / future work

- **Auto-render flamegraph in CI**: see optional section above. Defer until the raw-JFR download loop is shown to be the bottleneck.
- **Continuous profiling backend** (Pyroscope / Parca / Grafana Profiles): out of scope. Would replace the artifact-per-pipeline model with one that supports trend analysis across pipelines. Revisit only if appetite emerges.
- **Multiple recordings** (one short + high-detail for the first 10 min, one longer + lighter for the rest): only worth doing if the single-recording artifact turns out to be too large. With `maxsize=2g` and the 1 GB artifact cap, the practical ceiling is ~1 GB raw / ~350 MB gzipped, which should easily cover the full pipeline.
- **Filtering by thread name post-hoc**: not a design item but worth noting — JMC and `jfrconv --threads` both support this and it's how to untangle "what was test plan X doing" from "what was test plan Y doing" in a merged profile.
- **Additional events to consider disabling for redaction**: this design covers the five currently-known secret-carrying events (`jdk.InitialEnvironmentVariable`, `jdk.InitialSystemProperty`, `jdk.JVMInformation`, `jdk.SystemProcess`, `jdk.ProcessStart`). If the redaction sentinel check in Verification reveals any other leak vector (e.g. an event that captures HTTP headers including `Authorization`), add it to the disable list.
- **PVC orphan cleanup if the harvest job is canceled or never runs**: the pipeline-unique filenames mean a canceled `collect_jfr` leaves `/dump/cipipe-${pipelineId}*` behind, and the next pipeline can't reach into them because the filename is keyed to its own ID. Acceptable in v1 (PVC is 9.8 GB, a single max-sized recording is ~2 GB), but a periodic janitor or an init container that prunes files older than N days would be worth adding if profiled pipelines become routine.
- **Repository-chunk recovery for SIGKILL'd JVMs**: when a JVM is OOMKilled or otherwise SIGKILL'd, `dumponexit` never runs and the only surviving data is in `/dump/jfr-repo-${CI_PIPELINE_ID}/`. Those chunks are individually loadable JFR files and could be tarred up by the harvest job. Deferred from v1 because the chunks are mid-write at the moment of kill and stitching them carries its own correctness risks worth thinking through before automating.
