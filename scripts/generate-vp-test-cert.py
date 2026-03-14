#!/usr/bin/env python3
"""
Generate an EC P-256 signing key + self-signed certificate for VP integration tests.

The certificate includes the provided hostnames as SAN DNS entries, ensuring that
x509_san_dns client_id validation works regardless of the server's external hostname
(e.g. ngrok tunnel).

Outputs a JSON object with 'jwk' (JWK with x5c) suitable for patching test configs.
"""

import argparse
import base64
import datetime
import json
import sys

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509.oid import NameOID


# Default SAN DNS entries matching the existing test cert
DEFAULT_SANS = [
    "www.heenan.me.uk",
    "localhost",
    "localhost.emobix.co.uk",
    "demo.certification.openid.net",
    "www.certification.openid.net",
    "staging.certification.openid.net",
    "demo.pid-issuer.bundesdruckerei.de",
] + [
    f"review-app-dev-branch-{i}.certification.openid.net" for i in range(1, 31)
]


def generate_jwk_with_cert(extra_hostnames: list[str]) -> dict:
    """Generate an EC P-256 key + self-signed cert, return as JWK with x5c."""
    # Generate EC P-256 private key
    private_key = ec.generate_private_key(ec.SECP256R1())
    public_key = private_key.public_key()

    # Build SAN list
    all_sans = DEFAULT_SANS + [h for h in extra_hostnames if h not in DEFAULT_SANS]
    san_names = [x509.DNSName(name) for name in all_sans]

    # Build self-signed certificate
    subject = issuer = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, "GB"),
        x509.NameAttribute(NameOID.COMMON_NAME, "OIDF Test"),
    ])

    cert = (
        x509.CertificateBuilder()
        .subject_name(subject)
        .issuer_name(issuer)
        .public_key(public_key)
        .serial_number(x509.random_serial_number())
        .not_valid_before(datetime.datetime.now(datetime.timezone.utc))
        .not_valid_after(datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=3650))
        .add_extension(x509.SubjectAlternativeName(san_names), critical=False)
        .add_extension(x509.BasicConstraints(ca=True, path_length=None), critical=True)
        .add_extension(
            x509.SubjectKeyIdentifier.from_public_key(public_key),
            critical=False,
        )
        .add_extension(
            x509.AuthorityKeyIdentifier.from_issuer_public_key(public_key),
            critical=False,
        )
        .sign(private_key, hashes.SHA256())
    )

    # Export cert as DER -> base64 for x5c
    cert_der = cert.public_bytes(serialization.Encoding.DER)
    x5c_value = base64.b64encode(cert_der).decode("ascii")

    # Export key components for JWK
    private_numbers = private_key.private_numbers()
    public_numbers = private_numbers.public_numbers

    def int_to_base64url(n: int, length: int) -> str:
        return base64.urlsafe_b64encode(n.to_bytes(length, "big")).decode("ascii").rstrip("=")

    jwk = {
        "kty": "EC",
        "crv": "P-256",
        "x": int_to_base64url(public_numbers.x, 32),
        "y": int_to_base64url(public_numbers.y, 32),
        "d": int_to_base64url(private_numbers.private_value, 32),
        "use": "sig",
        "alg": "ES256",
        "x5c": [x5c_value],
    }

    return jwk


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--hostname", action="append", default=[], help="Extra hostname to add to cert SAN")
    parser.add_argument("--output", help="Write JWK to file (for use with {placeholder} substitution)")
    parser.add_argument("--json", action="store_true", help="Print JWK as JSON to stdout")
    args = parser.parse_args()

    if not args.hostname:
        print("No extra hostnames specified, nothing to do.", file=sys.stderr)
        sys.exit(0)

    jwk = generate_jwk_with_cert(args.hostname)

    if args.json:
        print(json.dumps(jwk, indent=2))

    if args.output:
        with open(args.output, "w") as f:
            json.dump(jwk, f)
            f.write("\n")
        print(f"Wrote {args.output}", file=sys.stderr)


if __name__ == "__main__":
    main()
