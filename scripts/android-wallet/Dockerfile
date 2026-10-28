# Android SDK + emulator image for building and running the EUDI reference wallet on tea.
# python3 is for scripts/android-wallet/run-wallet-tests.py.
# The SDK, Gradle caches, AVD and home directory live in the bind-mounted /w, not in the image.
FROM eclipse-temurin:21.0.12_8-jdk-noble

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      curl unzip git python3 \
      libpulse0 libnss3 libxcomposite1 libxcursor1 libxdamage1 libxi6 libxtst6 \
      libasound2t64 libgl1 libxkbfile1 libxrandr2 libgbm1 libdrm2 libxkbcommon0 \
 && rm -rf /var/lib/apt/lists/*

# cmdline-tools 23.0 (build 16111833), checksum from dl.google.com/android/repository/repository2-3.xml
RUN curl -fsSLo /tmp/ct.zip https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip \
 && echo "e025545c62a8e64c7559119566a569fb1dec5f60  /tmp/ct.zip" | sha1sum -c - \
 && mkdir -p /opt/android/cmdline-tools \
 && unzip -q /tmp/ct.zip -d /opt/android/cmdline-tools \
 && mv /opt/android/cmdline-tools/cmdline-tools /opt/android/cmdline-tools/latest \
 && rm /tmp/ct.zip

ENV ANDROID_HOME=/w/sdk \
    ANDROID_SDK_ROOT=/w/sdk \
    ANDROID_AVD_HOME=/w/avd \
    ANDROID_USER_HOME=/w/home/.android \
    GRADLE_USER_HOME=/w/gradle \
    HOME=/w/home \
    PATH=/opt/android/cmdline-tools/latest/bin:/w/sdk/platform-tools:/w/sdk/emulator:$PATH
