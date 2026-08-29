#!/usr/bin/env python3
"""
Generate EC P-256 signing keys with CA-signed certificates for VP/VCI
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
from cryptography.hazmat.primitives.asymmetric import ec
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


# keyUsage with only digitalSignature set, as ISO 18013-5 Table B.3 requires for DS certs
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
        # ISO 18013-5 Table B.1 IACA profile: pathLen 0, critical keyCertSign+cRLSign
        # keyUsage, issuerAltName with issuer contact information
        .add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True)
        .add_extension(
            x509.KeyUsage(
                digital_signature=False,
                content_commitment=False,
                key_encipherment=False,
                data_encipherment=False,
                key_agreement=False,
                key_cert_sign=True,
                crl_sign=True,
                encipher_only=False,
                decipher_only=False,
            ),
            critical=True,
        )
        .add_extension(
            x509.SubjectKeyIdentifier.from_public_key(ca_key.public_key()),
            critical=False,
        )
        .add_extension(
            x509.IssuerAlternativeName([x509.RFC822Name("certification@oidf.org")]),
            critical=False,
        )
        .sign(ca_key, hashes.SHA256())
    )
    return ca_key, ca_cert


def generate_ec_jwk(ca_key, ca_cert, san_names, mdoc_ds=False) -> dict:
    """Generate an EC P-256 leaf key + CA-signed cert, return as JWK with x5c.

    With mdoc_ds=False the cert is purpose-neutral (SD-JWT VC signing, OID4VP
    request-object signing): critical digitalSignature keyUsage, no EKU. With
    mdoc_ds=True the cert follows the ISO 18013-5 Table B.3 document signer
    profile (critical mdlDS EKU, issuerAltName, CRL distribution points,
    <=457 day validity) and must only be used for mdoc issuance.
    """
    private_key = ec.generate_private_key(ec.SECP256R1())

    validity_days = 457 if mdoc_ds else 3650
    builder = (
        x509.CertificateBuilder()
        .subject_name(x509.Name([
            x509.NameAttribute(NameOID.COUNTRY_NAME, "GB"),
            x509.NameAttribute(NameOID.COMMON_NAME, "OIDF Test mdoc DS" if mdoc_ds else "OIDF Test"),
        ]))
        .issuer_name(ca_cert.subject)
        .public_key(private_key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(datetime.datetime.now(datetime.timezone.utc))
        .not_valid_after(datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=validity_days))
        .add_extension(x509.SubjectAlternativeName(san_names), critical=False)
        .add_extension(x509.SubjectKeyIdentifier.from_public_key(private_key.public_key()), critical=False)
        .add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(ca_key.public_key()), critical=False)
        .add_extension(DIGITAL_SIGNATURE_ONLY, critical=True)
    )
    if mdoc_ds:
        builder = (
            builder
            .add_extension(x509.ExtendedKeyUsage([MDL_DS_OID]), critical=True)
            .add_extension(
                x509.IssuerAlternativeName([x509.RFC822Name("certification@oidf.org")]),
                critical=False,
            )
            .add_extension(
                x509.CRLDistributionPoints([
                    x509.DistributionPoint(
                        full_name=[x509.UniformResourceIdentifier("http://example.com/test-ca.crl")],
                        relative_name=None,
                        reasons=None,
                        crl_issuer=None,
                    )
                ]),
                critical=False,
            )
        )
    else:
        builder = builder.add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
    cert = builder.sign(ca_key, hashes.SHA256())

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


def generate_server_jwk(ca_key, ca_cert, san_names) -> dict:
    """Generate the EC P-256 server signing leaf + CA-signed cert, return as JWK with x5c.

    This key signs the suite's Token Status List tokens (the ISO 18013-5 MSO
    revocation list), so the cert follows the Table B.9 MSO revocation list
    signer profile: EC key, <=1187 day validity, SKI/AKI, critical
    digitalSignature-only keyUsage, all other extensions non-critical. The
    Token Status List EKU is omitted: it is optional in Table B.9 and the
    draft-ietf-oauth-status-list OID is still TBD. The subject copies the
    issuing CA's country (and state/province, if any) per B.1.1, which
    requires end-entity certs to match their IACA's values.
    """
    private_key = ec.generate_private_key(ec.SECP256R1())

    subject_attrs = []
    for oid in (NameOID.COUNTRY_NAME, NameOID.STATE_OR_PROVINCE_NAME):
        values = ca_cert.subject.get_attributes_for_oid(oid)
        if values:
            subject_attrs.append(x509.NameAttribute(oid, values[0].value))
    subject_attrs.append(x509.NameAttribute(NameOID.COMMON_NAME, "OIDF Test Server"))

    cert = (
        x509.CertificateBuilder()
        .subject_name(x509.Name(subject_attrs))
        .issuer_name(ca_cert.subject)
        .public_key(private_key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(datetime.datetime.now(datetime.timezone.utc))
        .not_valid_after(datetime.datetime.now(datetime.timezone.utc) + datetime.timedelta(days=1187))
        .add_extension(x509.SubjectAlternativeName(san_names), critical=False)
        .add_extension(x509.SubjectKeyIdentifier.from_public_key(private_key.public_key()), critical=False)
        .add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(ca_key.public_key()), critical=False)
        .add_extension(DIGITAL_SIGNATURE_ONLY, critical=True)
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
    parser.add_argument("--mdoc-output", help="Write an ISO 18013-5 Table B.3 mdoc DS signing JWK (same CA) to file")
    parser.add_argument("--server-output", help="Write EC server signing JWK (Table B.9 MSO revocation list signer profile) to file")
    parser.add_argument("--ca-output", help="Write CA trust anchor PEM to file")
    parser.add_argument("--ca-key-output", help="Write CA private key as EC JWK to file")
    parser.add_argument("--ca-key-input", help="Reuse an existing CA: EC private key JWK file (requires --ca-cert-input)")
    parser.add_argument("--ca-cert-input", help="Reuse an existing CA: certificate PEM file (requires --ca-key-input)")
    parser.add_argument("--json", action="store_true", help="Print EC credential JWK as JSON to stdout")
    args = parser.parse_args()

    if bool(args.ca_key_input) != bool(args.ca_cert_input):
        parser.error("--ca-key-input and --ca-cert-input must be used together")

    if not args.hostname and not args.ca_key_input:
        print("No extra hostnames specified, nothing to do.", file=sys.stderr)
        sys.exit(0)

    san_names = _build_san_names(args.hostname)
    if args.ca_key_input:
        with open(args.ca_key_input) as f:
            ca_jwk_in = json.load(f)
        ca_key = ec.derive_private_key(
            int.from_bytes(base64.urlsafe_b64decode(ca_jwk_in["d"] + "=="), "big"),
            ec.SECP256R1(),
        )
        with open(args.ca_cert_input, "rb") as f:
            ca_cert = x509.load_pem_x509_certificate(f.read())
    else:
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

    if args.mdoc_output:
        mdoc_jwk = generate_ec_jwk(ca_key, ca_cert, san_names, mdoc_ds=True)
        _write_jwk(mdoc_jwk, args.mdoc_output)

    if args.server_output:
        server_jwk = generate_server_jwk(ca_key, ca_cert, san_names)
        _write_jwk(server_jwk, args.server_output)

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
