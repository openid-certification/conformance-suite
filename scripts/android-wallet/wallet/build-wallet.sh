#!/bin/bash
# Build the EC reference wallet APK the android_wallet_test job installs, from a pinned commit of
# eudi-app-android-wallet-ui plus the patches in this directory, and name it after its SHA-256.
#
#   build-wallet.sh <work-dir> <out-dir>
#
# <work-dir> holds the wallet checkout (src) and is reused between builds. The APK is written to
# <out-dir>/ec-wallet-<first 16 hex digits of its SHA-256>.apk and the full digest is printed; pin
# that digest as WALLET_APK_SHA256 in .gitlab-ci.yml. Needs ANDROID_HOME with the packages below
# (sdkmanager installs any that are missing), JDK 21 and network access; see ../Dockerfile for the
# image this runs in on the tea runner.
set -euo pipefail
: "${ANDROID_HOME:?}"
WORK_DIR=${1:?work dir}
OUT_DIR=${2:?out dir}
HERE=$(cd "$(dirname "$0")" && pwd)

WALLET_REPO=https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui.git
WALLET_COMMIT=ad96068d806e781d3ec789e7b08992b79d530121
PATCHES=(launchpad-lote-urls.patch launchpad-log-failures.patch)

# yes exits by SIGPIPE once sdkmanager stops reading, which pipefail would report as a failure
yes 2>/dev/null | sdkmanager --licenses > /dev/null || true
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"

SRC=$WORK_DIR/src
if [ ! -d "$SRC/.git" ]; then
  git clone "$WALLET_REPO" "$SRC"
fi
git -C "$SRC" fetch -q origin
git -C "$SRC" checkout -q --force "$WALLET_COMMIT"
git -C "$SRC" clean -fdq -e build -e '*/build'
for p in "${PATCHES[@]}"; do
  git -C "$SRC" apply "$HERE/$p"
done

(cd "$SRC" && ./gradlew --no-daemon -q :app:assembleDemoDebug)

BUILT=$SRC/app/build/outputs/apk/demo/debug/app-demo-debug.apk
SHA=$(sha256sum "$BUILT" | cut -c1-64)
mkdir -p "$OUT_DIR"
cp "$BUILT" "$OUT_DIR/ec-wallet-${SHA:0:16}.apk"
echo "$OUT_DIR/ec-wallet-${SHA:0:16}.apk"
echo "WALLET_APK_SHA256: $SHA"
