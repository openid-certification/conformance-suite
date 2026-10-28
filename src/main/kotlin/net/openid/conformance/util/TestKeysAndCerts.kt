package net.openid.conformance.util

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.net.URI
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.io.bytestring.ByteString
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.AccessDescription
import org.bouncycastle.asn1.x509.AuthorityInformationAccess
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.CRLDistPoint
import org.bouncycastle.asn1.x509.CertificatePolicies
import org.bouncycastle.asn1.x509.DistributionPoint
import org.bouncycastle.asn1.x509.DistributionPointName
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.PolicyInformation
import org.bouncycastle.asn1.x509.PolicyQualifierInfo
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers
import org.bouncycastle.asn1.x509.qualified.ETSIQCObjectIdentifiers
import org.bouncycastle.asn1.x509.qualified.QCStatement
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKeyDoubleCoordinate
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.slf4j.LoggerFactory

/**
 * The suite's own PKI: the key material it signs with when it plays the emulated side of a test
 * — the issuer in OpenID4VCI wallet tests, the wallet in OpenID4VCI issuer and OpenID4VP verifier
 * tests, the verifier in OpenID4VP wallet tests — and the trust anchors an implementation under
 * test pins (or finds in a trusted list) to accept what the suite signs.
 *
 * Five fixed self-signed CAs ([SuiteCa]), whose certificates are checked in, issue short-lived leaf
 * certificates that are minted at runtime and re-minted daily; the private keys of all but the mdoc
 * IACA come from the deployment's environment (see [resolveIdentity]):
 *
 * - the mdoc IACA root (ISO/IEC 18013-5 Table B.1) issues the document signer certificate in each
 *   mdoc's x5chain (Table B.3) and the MSO revocation list signer (Table B.9);
 * - the PID provider CA issues the certificate that signs SD-JWT VC credentials;
 * - the wallet provider CA issues the certificate that signs the suite's wallet (client) attestations
 *   and key attestations;
 * - the access CA issues the suite's wallet-relying-party access certificate (WRPAC), which signs
 *   OpenID4VP authorization requests, authenticates the suite as an mdoc reader, and signs the
 *   suite's issuer metadata;
 * - the WRPRC provider CA issues the certificate that signs the suite's wallet-relying-party
 *   registration certificate (WRPRC) and its status list.
 *
 * The leaves follow the ETSI profiles the EUDI Wallet ecosystem validates against — ETSI TS 119 412-6
 * for PID and wallet provider sign/seal certificates (EN 319 412-3 subject with organizationName and
 * organizationIdentifier, an authority information access pointer to the issuing CA, a QcType
 * statement naming the role) and ETSI TS 119 411-8 for the WRPAC (certificate policy with CPS pointer,
 * contact details in the subject alternative name) — on top of the ISO/IEC 18013-5 Annex B profiles
 * where both apply. Only the leaf goes into x5c/x5chain; the CAs are published at /trust-anchors and
 * their CRLs at /crl (and the IACA's at /mdoc-iaca.crl), see SuiteCaCrl.
 *
 * Parameterizing this key material via the test configuration is tracked in issue #1663.
 */
object TestKeysAndCerts {

	/**
	 * The OpenID Foundation's organizationIdentifier as EN 319 412-1 clause 5.1.4 encodes a Legal
	 * Entity Identifier: the "LEI" type reference, the "XG" country code the clause fixes for LEIs,
	 * a hyphen and the LEI itself (OPENID FOUNDATION in the Global LEI Index).
	 */
	const val ORGANIZATION_IDENTIFIER: String = "LEIXG-984500451AC4373D4998"

	const val ORGANIZATION_NAME: String = "OpenID Foundation"

	const val CONTACT_EMAIL: String = "certification@oidf.org"
	const val CONTACT_URL: String = "https://openid.net/certification/"

	// Self-signed IACA root, ECDSA P-256, minted 2026-08-22, expires 2035-08-20 (9 years —
	// the ceiling ISO/IEC 18013-5 Table B.1's note gives for an mDL-only IACA). Before
	// regenerating (keep the key, mint a new self-signed cert) check the result against
	// Table B.1; the current cert satisfies it (critical basicConstraints CA:true/pathlen:0,
	// critical keyUsage keyCertSign+cRLSign, SKI, issuerAltName email, countryName + CN,
	// ECDSA-with-SHA256). The mattr checker at https://tools.mattrlabs.com/pem is useful.
	// Note: verifiers under test pin this certificate as a trust anchor (published at
	// /mdoc-iaca-root.pem) — regenerating it invalidates their configuration, so keep the
	// same key (the SKI, and so the DS leaves' AKIs, stay stable) and announce the change.
	const val IACA_ROOT_CERT_PEM: String = """-----BEGIN CERTIFICATE-----
MIICqjCCAlCgAwIBAgIUdV12Sp9RMJowoAxKoygBI2mDZ74wCgYIKoZIzj0EAwIw
gYcxCzAJBgNVBAYTAlVTMRgwFgYDVQQIDA9TdGF0ZSBvZiBVdG9waWExEjAQBgNV
BAcMCVNhbiBSYW1vbjEaMBgGA1UECgwRT3BlbklEIEZvdW5kYXRpb24xCzAJBgNV
BAsMAklUMSEwHwYDVQQDDBhjZXJ0aWZpY2F0aW9uLm9wZW5pZC5uZXQwHhcNMjYw
ODIyMTg1ODQ4WhcNMzUwODIwMTg1ODQ4WjCBhzELMAkGA1UEBhMCVVMxGDAWBgNV
BAgMD1N0YXRlIG9mIFV0b3BpYTESMBAGA1UEBwwJU2FuIFJhbW9uMRowGAYDVQQK
DBFPcGVuSUQgRm91bmRhdGlvbjELMAkGA1UECwwCSVQxITAfBgNVBAMMGGNlcnRp
ZmljYXRpb24ub3BlbmlkLm5ldDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJ5o
lgDBiHqNhN7rFkSy/xD34dQcOSR4KvEWMyb62jI+UGUofeAi/55RIt74pBsQz9+B
48WXI8xhIphoNN7AejajgZcwgZQwHQYDVR0OBBYEFHhk9LVVH8Gt9ZgfxgyhSl92
1XOhMBIGA1UdEwEB/wQIMAYBAf8CAQAwDgYDVR0PAQH/BAQDAgEGMCEGA1UdEgQa
MBiBFmNlcnRpZmljYXRpb25Ab2lkZi5vcmcwLAYDVR0fBCUwIzAhoB+gHYYbaHR0
cDovL2V4YW1wbGUuY29tL215Y2EuY3JsMAoGCCqGSM49BAMCA0gAMEUCIQDo0oJW
bRt3u+VoeErRDeMw0AC9srdovY3cmzyoXJAzbgIgG+kuZIK3UUQgwSrPzInxgdo9
ccSYJako6h4oBTJEY38=
-----END CERTIFICATE-----"""

	private const val IACA_ROOT_KEY_PEM = """-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg/ANvinTxJAdR8nQ0
NoUdBMcRJz+xLsb0kmhyMk+lkkGhRANCAASeaJYAwYh6jYTe6xZEsv8Q9+HUHDkk
eCrxFjMm+toyPlBlKH3gIv+eUSLe+KQbEM/fgePFlyPMYSKYaDTewHo2
-----END PRIVATE KEY-----"""

	// The three ETSI-profile CAs below were minted 2026-10-14 and expire 2035-10-12: self-signed
	// ECDSA P-256, subject C=US, O=OpenID Foundation, organizationIdentifier=<LEI>, CN=<role>, with
	// critical basicConstraints CA:true/pathlen:0, critical keyUsage keyCertSign+cRLSign and an SKI.
	// They are listed in the European Commission's test lists of trusted entities, so their private
	// keys are not in the source: a deployment supplies them in environment variables (see
	// [keyEnvironmentVariable]), and without them the suite signs with ephemeral stand-ins.

	private const val PID_PROVIDER_CA_CERT_PEM = """-----BEGIN CERTIFICATE-----
MIICOzCCAeGgAwIBAgIUE7pKERnd41sH3oDB0FL81qfMLnkwCgYIKoZIzj0EAwIw
ejELMAkGA1UEBhMCVVMxGjAYBgNVBAoMEU9wZW5JRCBGb3VuZGF0aW9uMSMwIQYD
VQRhDBpMRUlYRy05ODQ1MDA0NTFBQzQzNzNENDk5ODEqMCgGA1UEAwwhT3BlbklE
IEZvdW5kYXRpb24gUElEIFByb3ZpZGVyIENBMB4XDTI2MTAxNDAwMDAwMFoXDTM1
MTAxMjAwMDAwMFowejELMAkGA1UEBhMCVVMxGjAYBgNVBAoMEU9wZW5JRCBGb3Vu
ZGF0aW9uMSMwIQYDVQRhDBpMRUlYRy05ODQ1MDA0NTFBQzQzNzNENDk5ODEqMCgG
A1UEAwwhT3BlbklEIEZvdW5kYXRpb24gUElEIFByb3ZpZGVyIENBMFkwEwYHKoZI
zj0CAQYIKoZIzj0DAQcDQgAEhZ0fmXGbi0MW9t+kdb4/zL59LXFAFLqsxCQBv0XE
iLsPz2hozIITyaBxHY8NXs6DAVwYo7ugtAfSflPbElHUf6NFMEMwHQYDVR0OBBYE
FOgNGEDCiRA22oUBBLzrkat3E16gMBIGA1UdEwEB/wQIMAYBAf8CAQAwDgYDVR0P
AQH/BAQDAgEGMAoGCCqGSM49BAMCA0gAMEUCIQCy1ci2Xz9OriXwDGunIkYjl8wx
HFIGgiLsY0yRbfWr6AIgOyrG1/oPOToh3X6jPOec8OSCyw/1/e0nLvNF74bqs6I=
-----END CERTIFICATE-----"""


	private const val WALLET_PROVIDER_CA_CERT_PEM = """-----BEGIN CERTIFICATE-----
MIICQjCCAeegAwIBAgIUeJ+Ss/lWt4cJiE2HmEorwBMsWOQwCgYIKoZIzj0EAwIw
fTELMAkGA1UEBhMCVVMxGjAYBgNVBAoMEU9wZW5JRCBGb3VuZGF0aW9uMSMwIQYD
VQRhDBpMRUlYRy05ODQ1MDA0NTFBQzQzNzNENDk5ODEtMCsGA1UEAwwkT3BlbklE
IEZvdW5kYXRpb24gV2FsbGV0IFByb3ZpZGVyIENBMB4XDTI2MTAxNDAwMDAwMFoX
DTM1MTAxMjAwMDAwMFowfTELMAkGA1UEBhMCVVMxGjAYBgNVBAoMEU9wZW5JRCBG
b3VuZGF0aW9uMSMwIQYDVQRhDBpMRUlYRy05ODQ1MDA0NTFBQzQzNzNENDk5ODEt
MCsGA1UEAwwkT3BlbklEIEZvdW5kYXRpb24gV2FsbGV0IFByb3ZpZGVyIENBMFkw
EwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEPq3zLCV9abo0jmrrRojFzqusd8O8zTlR
UHb2Lzqeaez9ofJzQV2q2a1U+Cl+6riGRpYAfZRrVn9zmkk2tCOIYaNFMEMwHQYD
VR0OBBYEFMW6Fz9LO0e0tRAtJwZpmwY5D/UPMBIGA1UdEwEB/wQIMAYBAf8CAQAw
DgYDVR0PAQH/BAQDAgEGMAoGCCqGSM49BAMCA0kAMEYCIQCbzJDNAqdBRAi+NZFu
cV495Wv+BgmEsO+piXNueNzB4wIhAOxnMZk5E0Uc40KmA60QGzr2mHA7CGYX8ZxJ
WRmIVLdJ
-----END CERTIFICATE-----"""


	private const val ACCESS_CA_CERT_PEM = """-----BEGIN CERTIFICATE-----
MIICLzCCAdWgAwIBAgIUVQAiO4m7I4d0fYcoE/LFgtpGiBswCgYIKoZIzj0EAwIw
dDELMAkGA1UEBhMCVVMxGjAYBgNVBAoMEU9wZW5JRCBGb3VuZGF0aW9uMSMwIQYD
VQRhDBpMRUlYRy05ODQ1MDA0NTFBQzQzNzNENDk5ODEkMCIGA1UEAwwbT3BlbklE
IEZvdW5kYXRpb24gQWNjZXNzIENBMB4XDTI2MTAxNDAwMDAwMFoXDTM1MTAxMjAw
MDAwMFowdDELMAkGA1UEBhMCVVMxGjAYBgNVBAoMEU9wZW5JRCBGb3VuZGF0aW9u
MSMwIQYDVQRhDBpMRUlYRy05ODQ1MDA0NTFBQzQzNzNENDk5ODEkMCIGA1UEAwwb
T3BlbklEIEZvdW5kYXRpb24gQWNjZXNzIENBMFkwEwYHKoZIzj0CAQYIKoZIzj0D
AQcDQgAEkvPyf/1izGo31stSePpcCeRELkMt87aClKxgoz5d+9YsumZEJBq5EgII
4Q06yUSiV+77ng5gIOwa4tuvjPPciaNFMEMwHQYDVR0OBBYEFBS3rAkYzBJxJ9KC
N8/3Q+tRNkLEMBIGA1UdEwEB/wQIMAYBAf8CAQAwDgYDVR0PAQH/BAQDAgEGMAoG
CCqGSM49BAMCA0gAMEUCIQC1gqdzO6gtEENTR96u92tQCXH2wVI0tqasPT9+JCQb
JAIgWnI3xptHc6CENKJTTvGa23uRogXwpt5z7YZaXwwpO+4=
-----END CERTIFICATE-----"""


	// The WRPRC provider CA, minted 2026-10-15 and expiring 2035-10-13, with the same profile as the
	// three CAs above. It issues the leaf that signs the suite's wallet-relying-party registration
	// certificate and its status list (see SuiteWrprc). Its key comes from the environment as well.
	private const val WRPRC_PROVIDER_CA_CERT_PEM = """-----BEGIN CERTIFICATE-----
MIICQDCCAeWgAwIBAgIUa0ckOF41QHypgW40GcfKmnyJ4aEwCgYIKoZIzj0EAwIw
fDELMAkGA1UEBhMCVVMxGjAYBgNVBAoMEU9wZW5JRCBGb3VuZGF0aW9uMSMwIQYD
VQRhDBpMRUlYRy05ODQ1MDA0NTFBQzQzNzNENDk5ODEsMCoGA1UEAwwjT3BlbklE
IEZvdW5kYXRpb24gV1JQUkMgUHJvdmlkZXIgQ0EwHhcNMjYxMDE1MDAwMDAwWhcN
MzUxMDEzMDAwMDAwWjB8MQswCQYDVQQGEwJVUzEaMBgGA1UECgwRT3BlbklEIEZv
dW5kYXRpb24xIzAhBgNVBGEMGkxFSVhHLTk4NDUwMDQ1MUFDNDM3M0Q0OTk4MSww
KgYDVQQDDCNPcGVuSUQgRm91bmRhdGlvbiBXUlBSQyBQcm92aWRlciBDQTBZMBMG
ByqGSM49AgEGCCqGSM49AwEHA0IABEUWftLE2WHBGjRZjd8SS14wMHS7fU+RfJJf
T0AjZoaw7SHz0qGMxP1bCBJGejjN+acYockMQ1rXCYPdExZo+U6jRTBDMB0GA1Ud
DgQWBBRpyAL16fiH+7EZfVImogYDOKfykjASBgNVHRMBAf8ECDAGAQH/AgEAMA4G
A1UdDwEB/wQEAwIBBjAKBggqhkjOPQQDAgNJADBGAiEAvQKzCpJ823FkbKHbDTdL
Em2cp0p5hMBNtbz2ijuiCwMCIQCHDuMwYC76LA/cKXoy0YyCfMQCc9sHqHK5fFJ8
s1qUQw==
-----END CERTIFICATE-----"""


	// The certificate the suite seals its own lists of trusted entities with (see LoteBuilder): a
	// fixed self-signed P-256 signing certificate, valid 2026-10-14 to 2035-10-12, so that
	// consumers of the suite's lists can pin it the way they pin a scheme operator's sealing
	// certificate. Its countryName is EU, not US like the CAs': ETSI TS 119 602 clause 6.8.0 wants it
	// to equal the lists' SchemeTerritory, which the EU profiles fix at "EU". Not a CA: it signs
	// lists, not certificates. Its key, like the ETSI CAs', comes from the environment as
	// SUITE_CA_KEY_LOTE_SIGNER (see [keyEnvironmentVariable]).
	const val LOTE_SIGNER_CERT_PEM: String = """-----BEGIN CERTIFICATE-----
MIICUzCCAfmgAwIBAgIUIN27ddFJSDwGMfK3HZ3qfDO4a6MwCgYIKoZIzj0EAwIw
gYgxCzAJBgNVBAYTAkVVMRowGAYDVQQKDBFPcGVuSUQgRm91bmRhdGlvbjEjMCEG
A1UEYQwaTEVJWEctOTg0NTAwNDUxQUM0MzczRDQ5OTgxODA2BgNVBAMML09wZW5J
RCBGb3VuZGF0aW9uIGNvbmZvcm1hbmNlIHN1aXRlIExvVEUgc2lnbmVyMB4XDTI2
MTAxNDAwMDAwMFoXDTM1MTAxMjAwMDAwMFowgYgxCzAJBgNVBAYTAkVVMRowGAYD
VQQKDBFPcGVuSUQgRm91bmRhdGlvbjEjMCEGA1UEYQwaTEVJWEctOTg0NTAwNDUx
QUM0MzczRDQ5OTgxODA2BgNVBAMML09wZW5JRCBGb3VuZGF0aW9uIGNvbmZvcm1h
bmNlIHN1aXRlIExvVEUgc2lnbmVyMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE
ax+GKJ77aNMufop+5IOO/BdUDClJlNAsMyMlpNYIDWUt1ApOPjzrGYWy+2PnLjZ0
c9NutseGF+QLhvKtQj5+f6M/MD0wHQYDVR0OBBYEFJs/lflRe04kRWSWt7SK7J0L
0kemMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgeAMAoGCCqGSM49BAMCA0gA
MEUCIQDSqd1rf++s6eTJ+XL2BUviwUH66yNixTSpu3E+81sW0gIgQenBimeuREfz
6J1SfCRi42qyCw9we67rk6+q2n7HdpA=
-----END CERTIFICATE-----"""


	// mDL document signer OID, ISO/IEC 18013-5 Annex B: id-mdl-kp-mdlDS
	private const val MDL_DS_EKU_OID = "1.0.18013.5.1.2"

	// mdoc reader authentication OID, ISO/IEC 18013-5 Annex B: id-mdl-kp-mdlReaderAuth
	private const val MDL_READER_AUTH_EKU_OID = "1.0.18013.5.1.6"

	// ISO/IEC 23220-4 reader authentication OID for other mdoc document types
	private const val MDOC_READER_AUTH_EKU_OID = "1.0.23220.4.1.6"

	// ETSI TS 119 412-6 Annex A: QcType values for a PID provider and a wallet provider sign/seal certificate
	private const val QCT_PID_OID = "0.4.0.194126.1.1"
	private const val QCT_WAL_OID = "0.4.0.194126.1.2"

	// ETSI TS 119 411-8 clause 5.3: NCP-l-eudiwrp, the policy for access certificates issued to legal persons
	private const val NCP_L_EUDIWRP_POLICY_OID = "0.4.0.194118.1.2"

	// ETSI EN 319 411-1 clause 5.3: NCP, the normalized certificate policy; the sign/seal certificates of
	// EN 319 412-2 (which TS 119 412-6 builds on) shall carry a certificatePolicies extension (GEN-4.3.3-2)
	private const val NCP_POLICY_OID = "0.4.0.2042.1.1"

	/**
	 * One of the suite's CAs. [slug] names it in the URLs the suite serves it and its CRL at
	 * (/trust-anchors/{slug}.pem, /crl/{slug}.crl) and the environment variable its key is read from. The mdoc
	 * IACA root's key is in the source; the others' are not.
	 */
	enum class SuiteCa(val slug: String, val publishedCertPem: String, private val inlineKeyPem: String?) {
		MDOC_IACA_ROOT("mdoc-iaca-root", IACA_ROOT_CERT_PEM, IACA_ROOT_KEY_PEM),
		PID_PROVIDER("pid-provider-ca", PID_PROVIDER_CA_CERT_PEM, null),
		WALLET_PROVIDER("wallet-provider-ca", WALLET_PROVIDER_CA_CERT_PEM, null),
		ACCESS("access-ca", ACCESS_CA_CERT_PEM, null),
		WRPRC_PROVIDER("wrprc-provider-ca", WRPRC_PROVIDER_CA_CERT_PEM, null);

		/** The certificate and key in use: the published certificate, or an ephemeral stand-in. */
		val identity: SigningIdentity
			get() = identityOf(slug, publishedCertPem, inlineKeyPem, true)

		/** The certificate in use, PEM encoded. */
		val certPem: String
			get() = identity.certPem

		val cert: X509Cert
			get() = identity.cert

		val javaCert: X509Certificate
			get() = identity.javaCert

		val holder: X509CertificateHolder
			get() = identity.holder

		/** The CA's signing key: it issues the leaf certificates minted here and signs the CA's CRL. */
		val privateKey: PrivateKey
			get() = identity.privateKey

		/** The URL the suite serves this certificate at, PEM encoded, under the configured base URL. */
		val trustAnchorUrl: String
			get() = "$trustAnchorsUrl/$slug.pem"

		/**
		 * The URL the suite serves this certificate at DER encoded, which the leaves' authority
		 * information access names: RFC 5280 section 4.2.2.1 wants a single DER certificate there.
		 */
		val caIssuersUrl: String
			get() = "$trustAnchorsUrl/$slug.crt"

		/**
		 * The URL the suite serves this CA's CRL at. Plain http, as CRL distribution points are by
		 * PKI convention (RFC 5280 only describes HTTP, FTP and LDAP; Windows refuses https).
		 */
		val crlUrl: String
			get() = if (this == MDOC_IACA_ROOT) plainHttpBaseUrl + MdocIacaCrl.PATH else "$plainHttpBaseUrl/crl/$slug.crl"
	}

	/**
	 * A certificate with its private key that the suite signs certificates or lists with. [ephemeral]
	 * is true when the published certificate's key was not available and a stand-in was minted at
	 * startup: implementations that trust the published certificate will not accept what it signs.
	 */
	class SigningIdentity(val certPem: String, val privateKey: PrivateKey, val ephemeral: Boolean) {
		val cert: X509Cert = X509Cert.fromPem(certPem)

		val holder: X509CertificateHolder = X509CertificateHolder(cert.encoded.toByteArray())

		val javaCert: X509Certificate =
			CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(decodePemBody(certPem))) as X509Certificate
	}

	private val logger = LoggerFactory.getLogger(TestKeysAndCerts::class.java)

	private val identities = ConcurrentHashMap<String, SigningIdentity>()

	/**
	 * The environment variable a deployment supplies [slug]'s private key in, e.g.
	 * `SUITE_CA_KEY_PID_PROVIDER_CA`: the base64 encoding of the PKCS#8 key, either of its PEM file or of
	 * its DER bytes, so the value fits on one line and can be a masked CI/CD variable.
	 */
	@JvmStatic
	fun keyEnvironmentVariable(slug: String): String = "SUITE_CA_KEY_" + slug.uppercase().replace('-', '_')

	private fun identityOf(slug: String, publishedCertPem: String, inlineKeyPem: String?, ca: Boolean): SigningIdentity =
		identities.computeIfAbsent(slug) {
			resolveIdentity(slug, publishedCertPem, inlineKeyPem, ca, System.getenv(keyEnvironmentVariable(slug)))
		}

	/**
	 * The identity for [slug]: the published certificate with the key given inline, or with
	 * [encodedKey], the value of [keyEnvironmentVariable], which must belong to that certificate.
	 * Failing both, a freshly minted self-signed stand-in with the published subject and "(ephemeral)"
	 * appended to its common name, so a deployment without the keys still works, untrusted by anyone
	 * who pins the published certificate.
	 */
	@JvmStatic
	fun resolveIdentity(slug: String, publishedCertPem: String, inlineKeyPem: String?, ca: Boolean, encodedKey: String?): SigningIdentity {
		if (inlineKeyPem != null) {
			return SigningIdentity(publishedCertPem, parsePrivateKey(decodePemBody(inlineKeyPem)), false)
		}
		val variable = keyEnvironmentVariable(slug)
		if (!encodedKey.isNullOrBlank()) {
			val key = parsePrivateKey(decodeEncodedKey(variable, encodedKey))
			check(keyMatchesCertificate(key, publishedCertPem)) { "$variable is not the private key of the published $slug certificate" }
			return SigningIdentity(publishedCertPem, key, false)
		}
		logger.warn("Signing as an ephemeral {}: implementations that trust the published certificate will not accept what"
			+ " the suite signs with it. Set {} to the base64-encoded private key to use the published certificate.",
			slug, variable)
		return mintEphemeralIdentity(publishedCertPem, ca)
	}

	/** The PKCS#8 DER of a key given as base64 of its PEM file or of its DER bytes. */
	private fun decodeEncodedKey(variable: String, encodedKey: String): ByteArray {
		val decoded = try {
			Base64.getMimeDecoder().decode(encodedKey.trim())
		} catch (e: IllegalArgumentException) {
			throw IllegalStateException("$variable is not base64", e)
		}
		val text = String(decoded, Charsets.US_ASCII)
		return if (text.trimStart().startsWith("-----BEGIN")) decodePemBody(text) else decoded
	}

	private fun parsePrivateKey(pkcs8: ByteArray): PrivateKey =
		KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(pkcs8))

	private fun keyMatchesCertificate(key: PrivateKey, certPem: String): Boolean {
		val cert = CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(decodePemBody(certPem)))
		val probe = "suite key check".toByteArray()
		val signature = Signature.getInstance("SHA256withECDSA").apply { initSign(key); update(probe) }.sign()
		return Signature.getInstance("SHA256withECDSA").apply { initVerify(cert.publicKey); update(probe) }.verify(signature)
	}

	private fun mintEphemeralIdentity(publishedCertPem: String, ca: Boolean): SigningIdentity {
		val published = X509CertificateHolder(decodePemBody(publishedCertPem))
		val nameBuilder = X500NameBuilder(BCStyle.INSTANCE)
		for (rdn in published.subject.rdNs) {
			val attribute = rdn.first
			if (attribute.type == BCStyle.CN) {
				nameBuilder.addRDN(BCStyle.CN, attribute.value.toString() + " (ephemeral)")
			} else {
				nameBuilder.addRDN(attribute.type, attribute.value)
			}
		}
		return mintSelfSignedIdentity(nameBuilder.build(), ca)
	}

	/** A freshly minted self-signed P-256 certificate for [name], a CA when [ca], with its key; held only in memory. */
	private fun mintSelfSignedIdentity(name: X500Name, ca: Boolean): SigningIdentity {
		val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
		val now = System.currentTimeMillis()
		val builder = JcaX509v3CertificateBuilder(
			name, BigInteger(159, SecureRandom()).max(BigInteger.ONE),
			Date(now - 24L * 60 * 60 * 1000), Date(now + 9L * 365 * 24 * 60 * 60 * 1000), name, keyPair.public
		)
		builder.addExtension(Extension.subjectKeyIdentifier, false, JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.public))
		if (ca) {
			builder.addExtension(Extension.basicConstraints, true, BasicConstraints(0))
			builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign))
		} else {
			builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
			builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature))
		}
		val der = builder.build(JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private)).encoded
		val pem = "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(der) + "\n-----END CERTIFICATE-----"
		return SigningIdentity(pem, keyPair.private, true)
	}

	/**
	 * A leaf certificate with its private key, in the two shapes the suite signs with: a Nimbus
	 * [jwk] (with the certificate as its `x5c`) for JOSE and a multipaz key for COSE/mdoc.
	 */
	class MintedLeaf(val jwk: ECKey, val multipazKey: AsymmetricKey.X509CertifiedExplicit) {
		val cert: X509Cert
			get() = multipazKey.certChain.certificates.first()

		/** A JWK Set holding only [jwk] (private part included), in the shape the signing conditions take. */
		fun jwksJson(): JsonObject {
			val jwks = JsonObject()
			val keys = com.google.gson.JsonArray()
			keys.add(JsonParser.parseString(jwk.toJSONString()))
			jwks.add("keys", keys)
			return jwks
		}
	}

	@JvmStatic
	val iacaRootCert: X509Cert
		get() = SuiteCa.MDOC_IACA_ROOT.cert

	/** The IACA root as BouncyCastle sees it: the issuer of the certificates and CRLs minted here. */
	@JvmStatic
	val iacaRootCertHolder: X509CertificateHolder by lazy { X509CertificateHolder(iacaRootCert.encoded.toByteArray()) }

	/** The IACA root's subject key identifier: the authority key identifier of everything it issues. */
	@JvmStatic
	val iacaRootSubjectKeyIdentifier: ByteArray by lazy {
		SubjectKeyIdentifier.fromExtensions(iacaRootCertHolder.extensions).keyIdentifier
	}

	/** The sealing certificate and key of the suite's lists of trusted entities: the published one, or an ephemeral stand-in. */
	@JvmStatic
	val loteSigner: SigningIdentity
		get() = identityOf("lote-signer", LOTE_SIGNER_CERT_PEM, null, false)

	/** The sealing certificate of the suite's lists of trusted entities, as a Java certificate. */
	@JvmStatic
	val loteSignerCert: X509Certificate
		get() = loteSigner.javaCert

	/** The sealing certificate in use, PEM encoded. */
	@JvmStatic
	val loteSignerCertPem: String
		get() = loteSigner.certPem

	/** The sealing key of the suite's lists of trusted entities, with [loteSignerCert] as its x5c; the identity is fixed, so built once. */
	@JvmStatic
	val loteSignerJwk: ECKey by lazy {
		ECKey.Builder(Curve.P_256, loteSignerCert.publicKey as java.security.interfaces.ECPublicKey)
			.privateKey(loteSigner.privateKey)
			.algorithm(JWSAlgorithm.ES256)
			.x509CertChain(listOf(com.nimbusds.jose.util.Base64.encode(loteSignerCert.encoded)))
			.build()
	}

	/**
	 * The IACA's signing key: it issues the leaf certificates minted here and signs the CRL the
	 * suite serves for them (see MdocIacaCrl).
	 */
	@JvmStatic
	val iacaRootPrivateKey: PrivateKey
		get() = SuiteCa.MDOC_IACA_ROOT.privateKey

	@JvmStatic
	val documentSignerKey: AsymmetricKey.X509CertifiedExplicit
		get() = rotatingDocumentSigner.get().multipazKey

	@JvmStatic
	val documentSignerCert: X509Cert
		get() = documentSignerKey.certChain.certificates.first()

	/**
	 * Key for signing an MSO revocation list (a Status List Token in CWT format, see
	 * ISO/IEC 18013-5 12.3.6.3), with a runtime-minted certificate issued by the same IACA root
	 * as the document signer certificate. 12.3.6.2 says that when the MSO's status element
	 * carries no Certificate element — the suite does not include one — the top-level
	 * certificate of the revocation list's x5chain must be signed by the certificate that signed
	 * the certificate in the MSO's x5chain, i.e. by the IACA. The certificate follows the MSO
	 * revocation list signer profile in ISO/IEC 18013-5 Table B.9, which unlike the document
	 * signer profile has no extended key usage (the Token Status List EKU OID is still TBD) and
	 * no mandatory CRL distribution point.
	 */
	@JvmStatic
	val statusListSignerKey: AsymmetricKey.X509CertifiedExplicit
		get() = rotatingStatusListSigner.get().multipazKey

	@JvmStatic
	val statusListSignerCert: X509Cert
		get() = statusListSignerKey.certChain.certificates.first()

	/**
	 * The certificate (and key) the suite signs SD-JWT VC credentials with when the test
	 * configuration supplies no 'Signing JWK': a PID provider sign/seal certificate per ETSI
	 * TS 119 412-6 clause 4, issued by [SuiteCa.PID_PROVIDER].
	 */
	@JvmStatic
	val sdJwtPidSigner: MintedLeaf
		get() = rotatingSdJwtPidSigner.get()

	/**
	 * The certificate (and key) the suite signs its wallet attestations and key attestations
	 * with when the test configuration supplies no attester or key attestation JWKS: a wallet
	 * provider sign/seal certificate per ETSI TS 119 412-6 clause 5, issued by
	 * [SuiteCa.WALLET_PROVIDER].
	 */
	@JvmStatic
	val walletAttestationSigner: MintedLeaf
		get() = rotatingWalletAttestationSigner.get()

	/**
	 * The suite's wallet-relying-party access certificate (and key): signs OpenID4VP authorization
	 * requests and issuer metadata and authenticates the suite as an mdoc reader when the test
	 * configuration supplies no client JWKS. Follows ETSI TS 119 411-8 clause 6.6.1 for a legal
	 * person and the mdoc reader authentication profile in ISO/IEC 18013-5 Table B.6; issued by
	 * [SuiteCa.ACCESS]. Its subject alternative name carries the host of the configured base URL
	 * so it satisfies the x509_san_dns client identifier prefix.
	 */
	@JvmStatic
	val wrpac: MintedLeaf
		get() = rotatingWrpac.get()

	/**
	 * The certificate (and key) the suite signs its wallet-relying-party registration certificate and
	 * that certificate's status list with, issued by [SuiteCa.WRPRC_PROVIDER]. ETSI TS 119 475 defines
	 * no profile for it beyond being the provider's signing certificate, so it is a plain leaf.
	 */
	@JvmStatic
	val wrprcSigner: MintedLeaf
		get() = rotatingWrprcSigner.get()

	/**
	 * A self-signed CA that nobody trusts: minted in memory at first use, never written anywhere and
	 * never published. It is not a [SuiteCa], so neither /trust-anchors, /crl nor the suite's lists of
	 * trusted entities name it. It issues [untrustedWrpac].
	 */
	@JvmStatic
	val untrustedAccessCa: SigningIdentity by lazy {
		mintSelfSignedIdentity(legalPersonSubject("certification.openid.net untrusted access CA"), true)
	}

	/**
	 * A twin of [wrpac] issued by [untrustedAccessCa]: the same subject, subject alternative name,
	 * certificate policy and key usages, with its own key. Its authority information access and CRL
	 * distribution point name URLs under a slug the suite does not serve. A wallet that validates the
	 * trust chain of the request signing certificate rejects requests signed with it, and has no other
	 * reason to.
	 */
	@JvmStatic
	val untrustedWrpac: MintedLeaf
		get() = rotatingUntrustedWrpac.get()

	/**
	 * A leaf key that is re-minted daily (and so never served anywhere near its certificate's
	 * expiry): frequent rotation keeps regeneration a routine event, so nothing can quietly start
	 * depending on one specific leaf certificate — the CAs are the only stable things to pin.
	 */
	private class DailyRotatedKey(private val mint: () -> MintedLeaf) {
		private var cached: MintedLeaf? = null
		private var mintedAt: kotlin.time.Instant? = null

		/** Drops the cached key so the next [get] mints afresh. */
		@Synchronized
		fun reset() {
			cached = null
			mintedAt = null
		}

		@Synchronized
		fun get(): MintedLeaf {
			val current = cached
			val at = mintedAt
			if (current != null && at != null && Clock.System.now() < at + 1.days) {
				return current
			}
			return mint().also {
				cached = it
				mintedAt = Clock.System.now()
			}
		}
	}

	private val rotatingDocumentSigner = DailyRotatedKey { mintDocumentSigner() }
	private val rotatingStatusListSigner = DailyRotatedKey { mintStatusListSigner() }
	private val rotatingSdJwtPidSigner = DailyRotatedKey { mintSdJwtPidSigner() }
	private val rotatingWalletAttestationSigner = DailyRotatedKey { mintWalletAttestationSigner() }
	private val rotatingWrpac = DailyRotatedKey { mintWrpac() }
	private val rotatingWrprcSigner = DailyRotatedKey { mintWrprcSigner() }
	private val rotatingUntrustedWrpac = DailyRotatedKey { mintUntrustedWrpac() }

	private val rotatingLeaves = listOf(
		rotatingDocumentSigner, rotatingStatusListSigner, rotatingSdJwtPidSigner,
		rotatingWalletAttestationSigner, rotatingWrpac, rotatingWrprcSigner, rotatingUntrustedWrpac
	)

	/**
	 * The https base URL of this deployment, which the leaf certificates name in their authority
	 * information access pointers (where the suite serves the CAs) and the WRPAC in its subject
	 * alternative name. Set with [configureBaseUrls] at startup; the default only serves code that
	 * mints before then, such as unit tests.
	 */
	@JvmStatic
	var httpsBaseUrl: String = "https://localhost:8080"
		private set

	/**
	 * The plain-http origin the leaf certificates name in their CRL distribution points (see
	 * [SuiteCa.crlUrl]). Set with [configureBaseUrls] at startup.
	 */
	@JvmStatic
	var plainHttpBaseUrl: String = "http://localhost:8080"
		private set

	/** The URL of the index of the suite's CA certificates, under [httpsBaseUrl]; each CA's [SuiteCa.trustAnchorUrl] is below it. */
	@JvmStatic
	val trustAnchorsUrl: String
		get() = "$httpsBaseUrl/trust-anchors"

	/** The host the WRPAC's subject alternative name carries: that of [httpsBaseUrl]. */
	@JvmStatic
	val wrpacDnsName: String
		get() = URI(httpsBaseUrl).host ?: throw IllegalStateException("The base URL $httpsBaseUrl has no host")

	/**
	 * Sets the URLs the leaf certificates embed, re-minting every leaf so no certificate minted
	 * under the previous values stays in use. Called once at startup by the component that serves
	 * the CAs and CRLs, since it is the one that knows where they live, and before the first request:
	 * the memoised CRLs, lists of trusted entities and registration certificate embed these URLs
	 * too and are built when first served, not rebuilt here.
	 */
	@JvmStatic
	fun configureBaseUrls(httpsBaseUrl: String, plainHttpBaseUrl: String) {
		require(httpsBaseUrl.isNotBlank() && plainHttpBaseUrl.isNotBlank()) { "The leaf certificates need base URLs" }
		this.httpsBaseUrl = httpsBaseUrl.trimEnd('/')
		this.plainHttpBaseUrl = plainHttpBaseUrl.trimEnd('/')
		rotatingLeaves.forEach { it.reset() }
	}

	/**
	 * The URI the document signer certificate's CRL distribution point names: where the suite
	 * serves [MdocIacaCrl].
	 */
	@JvmStatic
	val crlDistributionPointUri: String
		get() = SuiteCa.MDOC_IACA_ROOT.crlUrl

	/**
	 * Mints an EC P-256 leaf certificate under [ca] with the parts every profile the suite follows
	 * has in common: a CSPRNG serial, a subject key identifier, an authority key identifier equal to
	 * the CA's subject key identifier, a critical key usage asserting digitalSignature only (ISO/IEC
	 * 18013-5 Annex B; key usage type C of EN 319 412-2 Table 1, which TS 119 412-6 PID-4.4.1-02
	 * allows), an authority information access pointer to where the suite serves the CA (TS 119 412-6
	 * PID-4.4.3) and a CRL distribution point naming the CA's CRL. notBefore sits a day back and
	 * notAfter 400 days ahead: the certificate is reused for a day and an MSO signed under it is
	 * valid for a year, so this covers every MSO (ISO/IEC 18013-5 9.3.1 step 5) and stays under the
	 * 457 days Table B.3 allows. [addProfileExtensions] adds whatever else the leaf's profile calls for.
	 */
	private fun mintLeaf(
		ca: SuiteCa,
		subject: X500Name,
		addProfileExtensions: (X509v3CertificateBuilder) -> Unit
	): MintedLeaf = mintLeaf(leafIssuer(ca), subject, addProfileExtensions = addProfileExtensions)

	private fun leafIssuer(ca: SuiteCa): LeafIssuer = LeafIssuer(ca.holder, ca.privateKey, ca.caIssuersUrl, ca.crlUrl)

	/** The CA a leaf is minted under, and the URLs the leaf's authority information access and CRL distribution point name. */
	private class LeafIssuer(
		val holder: X509CertificateHolder,
		val privateKey: PrivateKey,
		val caIssuersUrl: String,
		val crlUrl: String
	)

	private fun mintLeaf(
		ca: LeafIssuer,
		subject: X500Name,
		keyUsage: KeyUsage? = KeyUsage(KeyUsage.digitalSignature),
		addProfileExtensions: (X509v3CertificateBuilder) -> Unit
	): MintedLeaf {
		val leafEcKey = ECKeyGenerator(Curve.P_256).keyIDFromThumbprint(true).generate()

		// uniform on [0, 2^159); max(ONE) only guards the astronomically improbable zero
		val serial = BigInteger(159, SecureRandom()).max(BigInteger.ONE)

		val now = System.currentTimeMillis()
		val notBefore = Date(now - 24L * 60 * 60 * 1000)
		val notAfter = Date(now + 400L * 24 * 60 * 60 * 1000)

		val builder = X509v3CertificateBuilder(
			ca.holder.subject,
			serial,
			notBefore,
			notAfter,
			subject,
			SubjectPublicKeyInfo.getInstance(leafEcKey.toECPublicKey().encoded)
		)

		val extensionUtils = JcaX509ExtensionUtils()
		val caSki = SubjectKeyIdentifier.fromExtensions(ca.holder.extensions)
		builder.addExtension(
			Extension.subjectKeyIdentifier, false,
			extensionUtils.createSubjectKeyIdentifier(leafEcKey.toECPublicKey())
		)
		builder.addExtension(
			Extension.authorityKeyIdentifier, false,
			AuthorityKeyIdentifier(caSki.keyIdentifier)
		)
		if (keyUsage != null) {
			builder.addExtension(Extension.keyUsage, true, keyUsage)
		}
		builder.addExtension(
			Extension.authorityInfoAccess, false,
			AuthorityInformationAccess(
				AccessDescription(
					X509ObjectIdentifiers.id_ad_caIssuers,
					GeneralName(GeneralName.uniformResourceIdentifier, ca.caIssuersUrl)
				)
			)
		)
		// One distribution point with one URI; ISO/IEC 18013-5 Table B.3 forbids the reasons and cRLIssuer fields.
		builder.addExtension(
			Extension.cRLDistributionPoints, false,
			CRLDistPoint(
				arrayOf(
					DistributionPoint(
						DistributionPointName(GeneralNames(GeneralName(GeneralName.uniformResourceIdentifier, ca.crlUrl))),
						null,
						null
					)
				)
			)
		)

		addProfileExtensions(builder)

		val signer = JcaContentSignerBuilder("SHA256withECDSA").build(ca.privateKey)
		val leafDer = builder.build(signer).encoded
		val leafCert = X509Cert(ByteString(leafDer))

		val leafPrivateKey = EcPrivateKeyDoubleCoordinate(
			EcCurve.P256,
			leafEcKey.d.decode(),
			leafEcKey.x.decode(),
			leafEcKey.y.decode()
		)
		val jwk = ECKey.Builder(leafEcKey)
			.algorithm(JWSAlgorithm.ES256)
			.x509CertChain(listOf(com.nimbusds.jose.util.Base64.encode(leafDer)))
			.build()
		return MintedLeaf(jwk, AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(leafCert)), leafPrivateKey))
	}

	/**
	 * The subject of a leaf under the IACA: ISO/IEC 18013-5 Table B.3 wants countryName and
	 * stateOrProvinceName equal to the IACA's plus a common name, and EN 319 412-3 (via TS 119 412-6)
	 * the organization's name and identifier.
	 */
	private fun iacaLeafSubject(commonName: String): X500Name =
		X500NameBuilder(BCStyle.INSTANCE)
			.addRDN(BCStyle.C, "US")
			.addRDN(BCStyle.ST, "State of Utopia")
			.addRDN(BCStyle.L, "San Ramon")
			.addRDN(BCStyle.O, ORGANIZATION_NAME)
			.addRDN(BCStyle.ORGANIZATION_IDENTIFIER, ORGANIZATION_IDENTIFIER)
			.addRDN(BCStyle.CN, commonName)
			.build()

	/** The subject of a leaf under an ETSI-profile CA: the EN 319 412-3 legal person attributes (LEG-4.2.1-2). */
	private fun legalPersonSubject(commonName: String): X500Name =
		X500NameBuilder(BCStyle.INSTANCE)
			.addRDN(BCStyle.C, "US")
			.addRDN(BCStyle.O, ORGANIZATION_NAME)
			.addRDN(BCStyle.ORGANIZATION_IDENTIFIER, ORGANIZATION_IDENTIFIER)
			.addRDN(BCStyle.CN, commonName)
			.build()

	/**
	 * The certificatePolicies extension naming [policyOid] with a CPS pointer to [CONTACT_URL]: EN 319
	 * 412-2 GEN-4.3.3-2 wants the extension on every sign/seal certificate, and TS 119 411-8 GEN-6.6.1-06
	 * the pointer on an access certificate.
	 */
	private fun addCertificatePolicy(builder: X509v3CertificateBuilder, policyOid: String) {
		builder.addExtension(
			Extension.certificatePolicies, false,
			CertificatePolicies(PolicyInformation(ASN1ObjectIdentifier(policyOid), DERSequence(PolicyQualifierInfo(CONTACT_URL))))
		)
	}

	/**
	 * The qcStatements extension (RFC 3739) holding one QcType statement (EN 319 412-5) whose value
	 * is [qcTypeOid]: how TS 119 412-6 marks a certificate as a PID provider's (PID-4.5-01) or a
	 * wallet provider's (WAL-5.2-01).
	 */
	private fun addQcTypeStatement(builder: X509v3CertificateBuilder, qcTypeOid: String) {
		val qcType = QCStatement(ETSIQCObjectIdentifiers.id_etsi_qcs_QcType, DERSequence(ASN1ObjectIdentifier(qcTypeOid)))
		builder.addExtension(Extension.qCStatements, false, DERSequence(qcType))
	}

	/**
	 * MSO revocation list signer certificate, ISO/IEC 18013-5 Table B.9. Beyond what every leaf
	 * gets: no extended key usage — Table B.9 makes it optional and the OID it names
	 * (oauthStatusSigning) is still TBD in the Token Status List specification.
	 */
	private fun mintStatusListSigner(): MintedLeaf =
		mintLeaf(SuiteCa.MDOC_IACA_ROOT, iacaLeafSubject("certification.openid.net mdoc status list signer")) { }

	/**
	 * Document signer certificate, ISO/IEC 18013-5 Table B.3 and, as the certificate a PID provider
	 * signs mdoc PIDs with, ETSI TS 119 412-6 clause 4. Beyond what every leaf gets: the critical
	 * mdlDS extended key usage, the issuer alternative name, the PID provider QcType and the NCP
	 * certificate policy (Table B.3 allows further non-critical extensions).
	 */
	private fun mintDocumentSigner(): MintedLeaf =
		mintLeaf(SuiteCa.MDOC_IACA_ROOT, iacaLeafSubject("certification.openid.net mdoc DS")) { builder ->
			builder.addExtension(
				Extension.extendedKeyUsage, true,
				ExtendedKeyUsage(KeyPurposeId.getInstance(ASN1ObjectIdentifier(MDL_DS_EKU_OID)))
			)
			builder.addExtension(
				Extension.issuerAlternativeName, false,
				GeneralNames(GeneralName(GeneralName.rfc822Name, CONTACT_EMAIL))
			)
			addQcTypeStatement(builder, QCT_PID_OID)
			addCertificatePolicy(builder, NCP_POLICY_OID)
		}

	/** SD-JWT VC credential signer: a PID provider sign/seal certificate, ETSI TS 119 412-6 clause 4. */
	private fun mintSdJwtPidSigner(): MintedLeaf =
		mintLeaf(SuiteCa.PID_PROVIDER, legalPersonSubject("certification.openid.net PID provider")) { builder ->
			addQcTypeStatement(builder, QCT_PID_OID)
			addCertificatePolicy(builder, NCP_POLICY_OID)
		}

	/** Registration certificate signer, under [SuiteCa.WRPRC_PROVIDER]. */
	private fun mintWrprcSigner(): MintedLeaf =
		mintLeaf(SuiteCa.WRPRC_PROVIDER, legalPersonSubject("certification.openid.net WRPRC provider")) { }

	/** Wallet and key attestation signer: a wallet provider sign/seal certificate, ETSI TS 119 412-6 clause 5. */
	private fun mintWalletAttestationSigner(): MintedLeaf =
		mintLeaf(SuiteCa.WALLET_PROVIDER, legalPersonSubject("certification.openid.net wallet provider")) { builder ->
			addQcTypeStatement(builder, QCT_WAL_OID)
			addCertificatePolicy(builder, NCP_POLICY_OID)
		}

	/**
	 * Wallet-relying-party access certificate, ETSI TS 119 411-8 clause 6.6.1 for a legal person
	 * and ISO/IEC 18013-5 Table B.6. Beyond what every leaf gets: the NCP-l-eudiwrp certificate
	 * policy with the CPS pointer GEN-6.6.1-06 wants, the contact details GEN-6.6.1-07 wants in the
	 * subject alternative name (an email address and a URL) alongside the dNSName the x509_san_dns
	 * client identifier prefix matches, and the critical mdoc reader authentication extended key
	 * usages.
	 */
	private fun mintWrpac(): MintedLeaf =
		mintWrpac(leafIssuer(SuiteCa.ACCESS))

	/** [untrustedWrpac]: the access certificate profile under [untrustedAccessCa]. */
	private fun mintUntrustedWrpac(): MintedLeaf = mintWrpac(untrustedLeafIssuer())

	/** [untrustedAccessCa], with authority information access and CRL distribution point URLs the suite does not serve. */
	private fun untrustedLeafIssuer(): LeafIssuer =
		LeafIssuer(
			untrustedAccessCa.holder, untrustedAccessCa.privateKey,
			"$trustAnchorsUrl/$UNTRUSTED_ACCESS_CA_SLUG.crt", "$plainHttpBaseUrl/crl/$UNTRUSTED_ACCESS_CA_SLUG.crl"
		)

	/** Extensions [mintUntrustedTwin] mints for the twin instead of copying: they name its key and its issuer. */
	private val TWIN_MINTED_EXTENSIONS = setOf(
		Extension.subjectKeyIdentifier, Extension.authorityKeyIdentifier,
		Extension.authorityInfoAccess, Extension.cRLDistributionPoints
	)

	/**
	 * A twin of [original] issued by [untrustedAccessCa]: the same subject and every extension of [original]
	 * (subject alternative name, key usage, certificate policies, extended key usages and any others), with
	 * its own P-256 key and the subject and authority key identifiers, authority information access and CRL
	 * distribution point a leaf of [untrustedAccessCa] has. An x509_san_dns client identifier that matches
	 * [original] matches the twin, and a wallet that validates the trust chain of the request signing
	 * certificate rejects requests signed with it. Minted afresh on each call and held only in memory.
	 */
	@JvmStatic
	fun mintUntrustedTwin(original: X509Certificate): MintedLeaf {
		val originalHolder = X509CertificateHolder(original.encoded)
		val copiedExtensions = originalHolder.extensions?.extensionOIDs.orEmpty().filterNot { it in TWIN_MINTED_EXTENSIONS }
		return mintLeaf(untrustedLeafIssuer(), originalHolder.subject, keyUsage = null) { builder ->
			for (oid in copiedExtensions) {
				builder.copyAndAddExtension(oid, originalHolder.getExtension(oid).isCritical, originalHolder)
			}
		}
	}

	private const val UNTRUSTED_ACCESS_CA_SLUG = "untrusted-access-ca"

	private fun mintWrpac(issuer: LeafIssuer): MintedLeaf =
		mintLeaf(issuer, legalPersonSubject("certification.openid.net relying party")) { builder ->
			addCertificatePolicy(builder, NCP_L_EUDIWRP_POLICY_OID)
			builder.addExtension(
				Extension.subjectAlternativeName, false,
				GeneralNames(
					arrayOf(
						GeneralName(GeneralName.dNSName, wrpacDnsName),
						GeneralName(GeneralName.rfc822Name, CONTACT_EMAIL),
						GeneralName(GeneralName.uniformResourceIdentifier, CONTACT_URL)
					)
				)
			)
			builder.addExtension(
				Extension.extendedKeyUsage, true,
				ExtendedKeyUsage(
					arrayOf(
						KeyPurposeId.getInstance(ASN1ObjectIdentifier(MDL_READER_AUTH_EKU_OID)),
						KeyPurposeId.getInstance(ASN1ObjectIdentifier(MDOC_READER_AUTH_EKU_OID))
					)
				)
			)
		}

	private fun decodePemBody(pem: String): ByteArray {
		val body = pem.lines()
			.filterNot { it.startsWith("-----") }
			.joinToString("")
		return Base64.getMimeDecoder().decode(body)
	}
}
