# This dockerfile is used when running development builds of the server
#
# It is necessary to make selenium work when the server is running on
# within docker on localhost - selenium runs on the jvm container, so
# localhost points at the spring server, whereas selenium needs to talk
# to the ingress httpd.
#
# We install redir, and then the --fintechlabs.startredir=true option
# to java is used to launch it
FROM eclipse-temurin:17

RUN apt-get update && apt-get install -y \
    redir \
    # Playwright browser dependencies for Chromium
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2t64 \
    libatspi2.0-0 \
    && rm -rf /var/lib/apt/lists/*
