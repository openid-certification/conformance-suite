# EC reference wallet in CI

The `android_wallet_test` job runs the suite's OpenID4VCI and OpenID4VP HAIP wallet plans against the
EC reference wallet ([eudi-app-android-wallet-ui](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui),
demo flavour) in an Android emulator, driving the wallet over adb by screen text. It runs in every
branch pipeline against that pipeline's deployment and fails when any module's outcome differs from
`expected-ec-wallet.json`.

## Files

- `run-wallet-tests.py`: the driver. Standard library only. Runs the encrypted VCI HAIP plan, issues
  extra PIDs (the demo flavour presents each PID instance once), then the VP HAIP plan, and compares
  every module's suite result and wallet behaviour with `expected-ec-wallet.json`. Screenshots, screen
  texts, logcat and `results.json` go to `ARTIFACT_DIR`; each plan's log export zip goes to the job's
  working directory, where `compare_android_wallet` diffs it against master's like the other
  `compare_*` jobs (condition names and order, as a warning; outcomes are gated by the baseline).
- `start-emulator.sh`: creates an AVD, boots the emulator headless, verifies the wallet APK against
  `WALLET_APK_SHA256` and installs it.
- `vci-ec-wallet-config.json`, `vp-ec-wallet-config.json`: the test configurations. Trust comes from the
  Commission's development lists of trusted entities, so the deployment must hold the published suite
  CA keys (`SUITE_CA_KEY_*`), which review apps do.
- `expected-ec-wallet.json`: the expected outcome of every module, keyed by module name and variants.
  The job refuses to start on an empty file. To record or renew the baseline, run the pipeline with
  the CI variable `RECORD_BASELINE=1` (exactly `1`), review the run, and commit its
  `expected-from-run.json` artifact as this file. Every entry must give `wallet`, `status` and `result`.
  The job sets `SKIP_DEFERRED=1`: a deferred issuance stays pending in the wallet, which later retries it
  with that module's refresh token and DPoP key against whichever module is then running and interrupts it.
- `wallet/`: how the APK is built (below).
- `Dockerfile`: the `eudiw-android:1` image the job runs in (JDK 21, cmdline-tools, emulator libraries).

## The runner

The job needs the `android` tag: a self-hosted docker runner with `/dev/kvm` and a directory mounted
read-only at `/eudiw` holding

- `sdk/`: an Android SDK with `platform-tools`, `emulator`, `build-tools;36.0.0`,
  `platforms;android-37.0` and `system-images;android-35;google_apis;x86_64`,
- `out/`: the wallet APKs, one file per build, named `ec-wallet-<first 16 hex digits of SHA-256>.apk`.

Currently that is `tea`, runner "tea android (KVM)", with `/home/joseph/eudiw-ref-wallet` as `/eudiw`.
Several APKs can sit in `out/` side by side, so a branch that needs a different wallet build pins a
different `WALLET_APK_SHA256` without disturbing the others.

## Building the wallet APK

`wallet/build-wallet.sh <work-dir> <out-dir>` checks out the pinned commit (`WALLET_COMMIT` in the
script), applies the patches in `wallet/`, runs `:app:assembleDemoDebug` and writes
`<out-dir>/ec-wallet-<digest>.apk`, printing the full digest to pin in `.gitlab-ci.yml`. Run it in
the image from `Dockerfile` with the SDK directory mounted at `/w`:

```bash
docker build -t eudiw-android:1 scripts/android-wallet
docker run --rm --user "$(id -u):$(id -g)" -v /home/joseph/eudiw-ref-wallet:/w \
  -v "$PWD/scripts/android-wallet/wallet:/wallet:ro" eudiw-android:1 /wallet/build-wallet.sh /w/build /w/out
```

The work directory keeps the wallet checkout between builds (`/w/build/src`); the Gradle cache in
`/w/gradle` is shared, so a rebuild with a warm cache takes about five minutes. `sdkmanager` installs
the build packages into the shared `/w/sdk`, so it may also refresh `platform-tools` there.

The patches:

- `launchpad-lote-urls.patch`: points the PID, WRPAC and WRPRC lists of trusted entities at the
  Commission's development lists, where the suite's CAs are listed.
- `launchpad-log-failures.patch`: logs the cause of issuance failures to logcat (tag `LaunchpadDebug`),
  which the job keeps as an artifact.

The pinned APK (`391a85b7…`) is commit `ad96068d80` with both patches, built with this script on
31 October 2026. A debug build is not byte-for-byte reproducible, so a rebuild produces a new digest
to pin.

## Running it by hand

Set `CONFORMANCE_SERVER`, `CONFORMANCE_TOKEN`, `ANDROID_HOME`, `WALLET_APK_DIR`, `WALLET_APK_SHA256`
and `ANDROID_WORK_DIR`, run `start-emulator.sh`, then `run-wallet-tests.py` with the three JSON files.
`SKIP_DEFERRED=1` skips the deferred-issuance modules, `EXTRA_PIDS` sets how many PIDs to issue before
the VP plan (default 14), `WALLET_PIN` the throwaway PIN (default 147258).
