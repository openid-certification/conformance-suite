# This dockerfile is used when running development builds of the server
#
# It is necessary to make selenium work when the server is running on
# within docker on localhost - selenium runs on the jvm container, so
# localhost points at the spring server, whereas selenium needs to talk
# to the ingress httpd.
#
# We install redir, and then the --fintechlabs.startredir=true option
# to java is used to launch it
FROM node:22-slim AS node

FROM eclipse-temurin:21

# The Node.js that Playwright's driver runs on when the suite runs with
# -Dbrowser.engine=playwright; the suite's jar does not bundle one.
COPY --from=node /usr/local/bin/node /usr/local/bin/node
ENV PLAYWRIGHT_NODEJS_PATH=/usr/local/bin/node

# Alongside redir, the shared libraries Playwright's Chromium needs (the browser
# itself is downloaded by Playwright on first use). docker-compose-prebuilt-playwright.yml
# installs the same list.
RUN apt-get update && apt-get install -y \
    redir \
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
