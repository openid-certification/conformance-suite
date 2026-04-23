#!/usr/bin/env python3
"""
Generate EC P-256 and RSA signing keys with CA-signed certificates for VP/VCI
integration tests.

The certificate includes the provided hostnames as SAN DNS entries, ensuring that
x509_san_dns client_id validation works regardless of the server's external hostname
(e.g. ngrok tunnel).

Generates a CA + leaf certificate chain. The x5c in each JWK contains only the
leaf cert (not the CA), per HAIP which requires the trust anchor to not be included.
"""

import argparse
import base64
import datetime
import json
import sys

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, rsa
from cryptography.x509.oid import NameOID, ObjectIdentifier


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


# ISO 18013-5 mdl Document Signer Extended Key Usage OID
MDL_DS_OID = ObjectIdentifier("1.0.18013.5.1.2")


def _int_to_base64url(n: int, length: int) -> str:
    return base64.urlsafe_b64encode(n.to_bytes(length, "big")).decode("ascii").rstrip("=")


def _build_san_names(extra_hostnames):
    all_sans = DEFAULT_SANS + [h for h in extra_hostnames if h not in DEFAULT_SANS]
    return [x509.DNSName(name) for name in all_sans]


def generate_ca(san_names):
    """Generate a self-signed root CA certificate."""
    ca_key = ec.generate_private_key(ec.SECP256R1())
    ca_subject = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, "GB"),
        x509.NameAttribute(NameOID.COMMON_NAME, "OIDF Test CA"),
    ])
    ca_cert = (
        x509.CertificateBuilder()
        .subject_name(ca_subject)
        .issuer_name(ca_subject)
        .public_key(ca_key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(datetime.datetime.now(datetime.timezone.utc))
        .not_valid_after(datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=3650))
        .add_extension(x509.SubjectAlternativeName(san_names), critical=False)
        .add_extension(x509.BasicConstraints(ca=True, path_length=1), critical=True)
        .add_extension(
            x509.SubjectKeyIdentifier.from_public_key(ca_key.public_key()),
            critical=False,
        )
        .add_extension(
            x509.AuthorityKeyIdentifier.from_issuer_public_key(ca_key.public_key()),
            critical=False,
        )
        .sign(ca_key, hashes.SHA256())
    )
    return ca_key, ca_cert


def generate_ec_jwk(ca_key, ca_cert, san_names) -> dict:
    """Generate an EC P-256 leaf key + CA-signed cert, return as JWK with x5c."""
    private_key = ec.generate_private_key(ec.SECP256R1())

    cert = (
        x509.CertificateBuilder()
        .subject_name(x509.Name([
            x509.NameAttribute(NameOID.COUNTRY_NAME, "GB"),
            x509.NameAttribute(NameOID.COMMON_NAME, "OIDF Test"),
        ]))
        .issuer_name(ca_cert.subject)
        .public_key(private_key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(datetime.datetime.now(datetime.timezone.utc))
        .not_valid_after(datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=3650))
        .add_extension(x509.SubjectAlternativeName(san_names), critical=False)
        .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
        .add_extension(x509.SubjectKeyIdentifier.from_public_key(private_key.public_key()), critical=False)
        .add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(ca_key.public_key()), critical=False)
        .add_extension(x509.ExtendedKeyUsage([MDL_DS_OID]), critical=False)
        .sign(ca_key, hashes.SHA256())
    )

    x5c_value = base64.b64encode(cert.public_bytes(serialization.Encoding.DER)).decode("ascii")
    pn = private_key.private_numbers()

    return {
        "kty": "EC",
        "crv": "P-256",
        "x": _int_to_base64url(pn.public_numbers.x, 32),
        "y": _int_to_base64url(pn.public_numbers.y, 32),
        "d": _int_to_base64url(pn.private_value, 32),
        "use": "sig",
        "alg": "ES256",
        "x5c": [x5c_value],
    }


def generate_rsa_jwk(ca_key, ca_cert, san_names) -> dict:
    """Generate an RSA 2048 leaf key + CA-signed cert, return as JWK with x5c."""
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)

    cert = (
        x509.CertificateBuilder()
        .subject_name(x509.Name([
            x509.NameAttribute(NameOID.COUNTRY_NAME, "GB"),
            x509.NameAttribute(NameOID.COMMON_NAME, "OIDF Test Server"),
        ]))
        .issuer_name(ca_cert.subject)
        .public_key(private_key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(datetime.datetime.now(datetime.timezone.utc))
        .not_valid_after(datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=3650))
        .add_extension(x509.SubjectAlternativeName(san_names), critical=False)
        .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
        .add_extension(x509.SubjectKeyIdentifier.from_public_key(private_key.public_key()), critical=False)
        .add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(ca_key.public_key()), critical=False)
        .sign(ca_key, hashes.SHA256())
    )

    x5c_value = base64.b64encode(cert.public_bytes(serialization.Encoding.DER)).decode("ascii")
    pn = private_key.private_numbers()
    pub = pn.public_numbers
    n_bytes = (pub.n.bit_length() + 7) // 8

    return {
        "kty": "RSA",
        "n": _int_to_base64url(pub.n, n_bytes),
        "e": _int_to_base64url(pub.e, 3),
        "d": _int_to_base64url(pn.d, n_bytes),
        "p": _int_to_base64url(pn.p, (pn.p.bit_length() + 7) // 8),
        "q": _int_to_base64url(pn.q, (pn.q.bit_length() + 7) // 8),
        "dp": _int_to_base64url(pn.dmp1, (pn.dmp1.bit_length() + 7) // 8),
        "dq": _int_to_base64url(pn.dmq1, (pn.dmq1.bit_length() + 7) // 8),
        "qi": _int_to_base64url(pn.iqmp, (pn.iqmp.bit_length() + 7) // 8),
        "use": "sig",
        "alg": "PS256",
        "x5c": [x5c_value],
    }


def _write_jwk(jwk, path):
    with open(path, "w") as f:
        json.dump(jwk, f)
        f.write("\n")
    print(f"Wrote {path}", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--hostname", action="append", default=[], help="Extra hostname to add to cert SAN")
    parser.add_argument("--output", help="Write EC credential signing JWK to file")
    parser.add_argument("--second-output", help="Write a second EC credential signing JWK (different key, same CA) to file")
    parser.add_argument("--server-output", help="Write RSA server signing JWK to file")
    parser.add_argument("--ca-output", help="Write CA trust anchor PEM to file")
    parser.add_argument("--ca-key-output", help="Write CA private key as EC JWK to file")
    parser.add_argument("--json", action="store_true", help="Print EC credential JWK as JSON to stdout")
    args = parser.parse_args()

    if not args.hostname:
        print("No extra hostnames specified, nothing to do.", file=sys.stderr)
        sys.exit(0)

    san_names = _build_san_names(args.hostname)
    ca_key, ca_cert = generate_ca(san_names)
    ca_pem = ca_cert.public_bytes(serialization.Encoding.PEM).decode("ascii")

    ec_jwk = generate_ec_jwk(ca_key, ca_cert, san_names)

    if args.json:
        print(json.dumps(ec_jwk, indent=2))

    if args.output:
        _write_jwk(ec_jwk, args.output)

    if args.second_output:
        ec_jwk_2 = generate_ec_jwk(ca_key, ca_cert, san_names)
        _write_jwk(ec_jwk_2, args.second_output)

    if args.server_output:
        rsa_jwk = generate_rsa_jwk(ca_key, ca_cert, san_names)
        _write_jwk(rsa_jwk, args.server_output)

    if args.ca_output:
        with open(args.ca_output, "w") as f:
            f.write(ca_pem)
        print(f"Wrote CA cert to {args.ca_output}", file=sys.stderr)
    else:
        print("\nCA Trust Anchor PEM:", file=sys.stderr)
        print(ca_pem, file=sys.stderr)

    if args.ca_key_output:
        pn = ca_key.private_numbers()
        ca_jwk = {
            "kty": "EC",
            "crv": "P-256",
            "x": _int_to_base64url(pn.public_numbers.x, 32),
            "y": _int_to_base64url(pn.public_numbers.y, 32),
            "d": _int_to_base64url(pn.private_value, 32),
        }
        _write_jwk(ca_jwk, args.ca_key_output)


if __name__ == "__main__":
    main()
