#!/bin/bash
# Boot a fresh emulator from the read-only SDK at $ANDROID_HOME and install the pinned wallet APK.
# The AVD and adb state live under $ANDROID_WORK_DIR, so each run starts from a clean wallet.
set -euo pipefail
: "${ANDROID_HOME:?}" "${WALLET_APK_DIR:?}" "${WALLET_APK_SHA256:?}" "${ANDROID_WORK_DIR:?}"
# the APK is named after its digest (see wallet/build-wallet.sh), so a different build never installs by accident
WALLET_APK=$WALLET_APK_DIR/ec-wallet-${WALLET_APK_SHA256:0:16}.apk
echo "$WALLET_APK_SHA256  $WALLET_APK" | sha256sum -c -
export ANDROID_SDK_ROOT=$ANDROID_HOME
export ANDROID_AVD_HOME=$ANDROID_WORK_DIR/avd
export ANDROID_USER_HOME=$ANDROID_WORK_DIR/home/.android
export HOME=$ANDROID_WORK_DIR/home
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH
mkdir -p "$ANDROID_AVD_HOME" "$ANDROID_USER_HOME"

echo no | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd --force -n ci \
  -k "system-images;android-35;google_apis;x86_64" -d pixel_6

emulator -accel-check
emulator -avd ci -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect -accel on \
  -cores 2 -memory 4096 > "$ANDROID_WORK_DIR/emulator.log" 2>&1 &
EMULATOR_PID=$!

# bounded, and abandoned as soon as the emulator process dies, so a broken emulator fails in minutes, not at the job timeout
for i in $(seq 1 120); do
  kill -0 "$EMULATOR_PID" 2>/dev/null || { echo "emulator exited"; tail -50 "$ANDROID_WORK_DIR/emulator.log"; exit 1; }
  [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && break
  sleep 5
done
[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] || { echo "emulator did not boot"; tail -50 "$ANDROID_WORK_DIR/emulator.log"; exit 1; }
echo "emulator booted after about $((i * 5)) s"
adb install -r "$WALLET_APK"
