#!/usr/bin/env bash
#
# Generate a self-signed TLS certificate and key under ./certs, for Cerbos
# (server.tls in cerbos-config/config.yaml) and for running the AuthZEN adapter
# outside a container (TLS_CERT/TLS_KEY). The adapter's docker image generates
# its own pair at build time and does not need this. CN/SAN localhost +
# 127.0.0.1, 365-day validity. The certs/ directory is gitignored — run this
# after cloning if you need a pair on the host.
#
#   ./generate-certs.sh
#
set -euo pipefail
cd "$(dirname "$0")"

mkdir -p certs

openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout certs/key.pem -out certs/cert.pem -days 365 \
  -subj "/CN=localhost/O=Cerbos Local" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1"

chmod 600 certs/key.pem
echo "Wrote certs/cert.pem and certs/key.pem (CN/SAN localhost, IP 127.0.0.1; valid 365 days)."
