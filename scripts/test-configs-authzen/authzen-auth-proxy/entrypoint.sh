#!/bin/sh
# Generates the proxy's self-signed TLS certificate at start-up (TLS material for the local
# AuthZEN rig is never committed), then runs nginx with the configuration next to this script.
set -e
# openssl writes key-generation progress to stderr; show it only if generation fails.
if ! output=$(openssl req -x509 -newkey rsa:2048 -nodes -days 3650 \
    -subj "/CN=authzen-auth-proxy" -addext "subjectAltName=DNS:authzen-auth-proxy" \
    -keyout /tmp/authzen-auth-proxy.key -out /tmp/authzen-auth-proxy.crt 2>&1); then
    echo "Generating the proxy's TLS certificate failed:" >&2
    echo "$output" >&2
    exit 1
fi
exec nginx -c /etc/nginx/authzen/nginx.conf -g 'daemon off;'
