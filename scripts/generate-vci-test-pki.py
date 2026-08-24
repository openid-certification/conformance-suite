#!/usr/bin/env python3
"""
Generate (and re-mint) the test PKI embedded inline in the VCI CI test configs
(scripts/test-configs-rp-against-op/vci-*.json).

One EC P-256 key is shared, under different kids, by the credential signing JWK,
the key attestation JWKS and the client attester JWK in those configs; a single
certificate for that key is chained to the "OpenID4VCI Conformance Tests Root".
This script mints a root satisfying the ISO/IEC 18013-5 Table B.1 IACA profile
and a leaf satisfying the Table B.3 document signer profile (the leaf also signs
SD-JWT credentials, key attestations and client attestations, none of which add
certificate profile requirements), then rewrites every occurrence in the configs.

Key reuse:
- The leaf key is read from the configs themselves (credential.signing_jwk), so
  re-minting only changes certificates - the private keys in the configs stay.
- The root key is persisted in scripts/certs-keys/vci-test-root-key.json, so the
  trust anchors in the configs only change when that file is deleted.

The leaf is valid for 457 days (the Table B.3 maximum), so re-run this script
(with --update-configs) roughly yearly; VpSigningCertMdocDsProfile_UnitTest-style
config drift is caught by the conditions in CI.
"""

import argparse
import base64
import datetime
import glob
import json
import os
import re
import sys

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509.oid import NameOID, ObjectIdentifier

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
CERTS_DIR = os.path.join(SCRIPT_DIR, "certs-keys")
CONFIG_GLOB = os.path.join(SCRIPT_DIR, "test-configs-rp-against-op", "vci-*.json")
ROOT_KEY_PATH = os.path.join(CERTS_DIR, "vci-test-root-key.json")
ROOT_CERT_PATH = os.path.join(CERTS_DIR, "vci-test-root.crt")

ROOT_COMMON_NAME = "OpenID4VCI Conformance Tests Root"
COUNTRY = "DE"

MDL_DS_OID = ObjectIdentifier("1.0.18013.5.1.2")

DIGITAL_SIGNATURE_ONLY = x509.KeyUsage(
    digital_signature=True,
    content_commitment=False,
    key_encipherment=False,
    data_encipherment=False,
    key_agreement=False,
    key_cert_sign=False,
    crl_sign=False,
    encipher_only=False,
    decipher_only=False,
)


def _b64url_to_int(value: str) -> int:
    return int.from_bytes(base64.urlsafe_b64decode(value + "=" * (-len(value) % 4)), "big")


def _b64url(n: int, length: int) -> str:
    return base64.urlsafe_b64encode(n.to_bytes(length, "big")).decode("ascii").rstrip("=")


def load_leaf_key_from_configs():
    # the configs contain {TEMPLATE} placeholders and are not strict JSON, so extract the
    # ct_credential_signing_key JWK fields by regex
    config = os.path.join(SCRIPT_DIR, "test-configs-rp-against-op", "vci-wallet-test-config-plain.json")
    with open(config) as f:
        content = f.read()
    block = re.search(
        r'"signing_jwk"\s*:\s*\{(.*?)"x5c"', content, re.S).group(1)
    fields = dict(re.findall(r'"([xyd])"\s*:\s*"([^"]+)"', block))
    private_numbers = ec.EllipticCurvePrivateNumbers(
        _b64url_to_int(fields["d"]),
        ec.EllipticCurvePublicNumbers(
            _b64url_to_int(fields["x"]), _b64url_to_int(fields["y"]), ec.SECP256R1()
        ),
    )
    return private_numbers.private_key()


def load_or_create_root_key():
    if os.path.exists(ROOT_KEY_PATH):
        with open(ROOT_KEY_PATH) as f:
            jwk = json.load(f)
        private_numbers = ec.EllipticCurvePrivateNumbers(
            _b64url_to_int(jwk["d"]),
            ec.EllipticCurvePublicNumbers(
                _b64url_to_int(jwk["x"]), _b64url_to_int(jwk["y"]), ec.SECP256R1()
            ),
        )
        print("Reusing root key from vci-test-root-key.json (trust anchor unchanged)")
        return private_numbers.private_key()
    root_key = ec.generate_private_key(ec.SECP256R1())
    pn = root_key.private_numbers()
    with open(ROOT_KEY_PATH, "w") as f:
        json.dump({
            "kty": "EC", "crv": "P-256",
            "x": _b64url(pn.public_numbers.x, 32),
            "y": _b64url(pn.public_numbers.y, 32),
            "d": _b64url(pn.private_value, 32),
        }, f, indent=2)
        f.write("\n")
    print("Generated NEW root key - the trust anchors in the configs change")
    return root_key


def load_or_create_root_cert(root_key):
    if os.path.exists(ROOT_CERT_PATH):
        with open(ROOT_CERT_PATH, "rb") as f:
            existing = x509.load_pem_x509_certificate(f.read())
        if existing.public_key().public_numbers() == root_key.public_key().public_numbers():
            print("Reusing existing vci-test-root.crt")
            return existing
    now = datetime.datetime.now(datetime.timezone.utc)
    subject = x509.Name([
        x509.NameAttribute(NameOID.COUNTRY_NAME, COUNTRY),
        x509.NameAttribute(NameOID.COMMON_NAME, ROOT_COMMON_NAME),
    ])
    # ISO 18013-5 Table B.1 IACA profile; 9 years per the mDL-only note
    root_cert = (
        x509.CertificateBuilder()
        .subject_name(subject)
        .issuer_name(subject)
        .public_key(root_key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(now)
        .not_valid_after(now + datetime.timedelta(days=9 * 365))
        .add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True)
        .add_extension(
            x509.KeyUsage(
                digital_signature=False, content_commitment=False, key_encipherment=False,
                data_encipherment=False, key_agreement=False, key_cert_sign=True,
                crl_sign=True, encipher_only=False, decipher_only=False),
            critical=True,
        )
        .add_extension(x509.SubjectKeyIdentifier.from_public_key(root_key.public_key()), critical=False)
        .add_extension(
            x509.IssuerAlternativeName([x509.RFC822Name("certification@oidf.org")]),
            critical=False,
        )
        .sign(root_key, hashes.SHA256())
    )
    with open(ROOT_CERT_PATH, "wb") as f:
        f.write(root_cert.public_bytes(serialization.Encoding.PEM))
    return root_cert


def mint_leaf(root_key, root_cert, leaf_key):
    now = datetime.datetime.now(datetime.timezone.utc)
    return (
        x509.CertificateBuilder()
        .subject_name(x509.Name([
            x509.NameAttribute(NameOID.COUNTRY_NAME, COUNTRY),
            x509.NameAttribute(NameOID.ORGANIZATION_NAME, "Example Issuer"),
            x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, "OID4VCI"),
            x509.NameAttribute(NameOID.COMMON_NAME, "issuer.example"),
        ]))
        .issuer_name(root_cert.subject)
        .public_key(leaf_key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(now)
        # ISO 18013-5 Table B.3 caps DS cert validity at 457 days
        .not_valid_after(now + datetime.timedelta(days=457))
        .add_extension(
            x509.SubjectAlternativeName([
                x509.DNSName("issuer.example"),
                x509.DNSName("localhost"),
            ]),
            critical=False,
        )
        .add_extension(x509.SubjectKeyIdentifier.from_public_key(leaf_key.public_key()), critical=False)
        .add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(root_key.public_key()), critical=False)
        .add_extension(DIGITAL_SIGNATURE_ONLY, critical=True)
        .add_extension(x509.ExtendedKeyUsage([MDL_DS_OID]), critical=True)
        .add_extension(
            x509.IssuerAlternativeName([x509.RFC822Name("certification@oidf.org")]),
            critical=False,
        )
        .add_extension(
            x509.CRLDistributionPoints([
                x509.DistributionPoint(
                    full_name=[x509.UniformResourceIdentifier("http://example.com/vci-test-root.crl")],
                    relative_name=None, reasons=None, crl_issuer=None)
            ]),
            critical=False,
        )
        .sign(root_key, hashes.SHA256())
    )


def update_configs(root_cert, leaf_cert):
    """Replace every certificate chained to (any version of) the test root in the configs."""
    new_root_pem_json = root_cert.public_bytes(serialization.Encoding.PEM).decode().strip().replace("\n", "\\n") + "\\n"
    new_leaf_b64 = base64.b64encode(leaf_cert.public_bytes(serialization.Encoding.DER)).decode("ascii")

    def is_ours(der):
        try:
            cert = x509.load_der_x509_certificate(der)
        except Exception:
            return None
        names = {attribute.value for attribute in cert.issuer} | {attribute.value for attribute in cert.subject}
        return cert if ROOT_COMMON_NAME in names else None

    for config in sorted(glob.glob(CONFIG_GLOB)):
        with open(config) as f:
            content = f.read()
        replaced = 0
        for pem_body in set(re.findall(r'-----BEGIN CERTIFICATE-----\\n([^"]+?)-----END CERTIFICATE-----\\n', content)):
            der = base64.b64decode("".join(pem_body.replace("\\n", "\n").split()))
            cert = is_ours(der)
            if cert is None:
                continue
            old = "-----BEGIN CERTIFICATE-----\\n" + pem_body + "-----END CERTIFICATE-----\\n"
            if cert.public_key().public_numbers() == root_cert.public_key().public_numbers() or \
               x509.load_der_x509_certificate(der).subject.rfc4514_string() == root_cert.subject.rfc4514_string() or \
               ROOT_COMMON_NAME in {attribute.value for attribute in cert.subject}:
                if old != new_root_pem_json:
                    replaced += content.count(old)
                    content = content.replace(old, new_root_pem_json)
        for x5c_b64 in set(re.findall(r'"(MI[A-Za-z0-9+/=]{200,})"', content)):
            cert = is_ours(base64.b64decode(x5c_b64))
            if cert is None:
                continue
            if x5c_b64 != new_leaf_b64:
                replaced += content.count(x5c_b64)
                content = content.replace(x5c_b64, new_leaf_b64)
        if replaced:
            with open(config, "w") as f:
                f.write(content)
            print(f"{os.path.basename(config)}: replaced {replaced} certificate value(s)")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--update-configs", action="store_true",
                        help="rewrite the vci-*.json test configs with the new certificates")
    arguments = parser.parse_args()

    leaf_key = load_leaf_key_from_configs()
    root_key = load_or_create_root_key()
    root_cert = load_or_create_root_cert(root_key)
    leaf_cert = mint_leaf(root_key, root_cert, leaf_key)
    print(f"Leaf valid until {leaf_cert.not_valid_after}")

    if arguments.update_configs:
        update_configs(root_cert, leaf_cert)
    else:
        print("Run with --update-configs to rewrite the test configs.", file=sys.stderr)


if __name__ == "__main__":
    main()
