package net.openid.conformance.util

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import kotlinx.io.bytestring.ByteString
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import org.bouncycastle.asn1.x509.CRLDistPoint
import org.bouncycastle.asn1.x509.DistributionPoint
import org.bouncycastle.asn1.x509.DistributionPointName
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKeyDoubleCoordinate
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import java.math.BigInteger
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Key material used when the test suite itself creates mdoc credentials — as the emulated
 * wallet in OpenID4VP verifier tests and as the emulated issuer in OpenID4VCI wallet tests.
 *
 * The IACA root below is a fixed, checked-in self-signed CA certificate (ISO/IEC 18013-5
 * Table B.1 profile). The document signer (DS) certificate embedded in each mdoc's
 * x5chain is minted at runtime under that root, following the DS certificate profile in
 * ISO/IEC 18013-5 Table B.3 (keyUsage digitalSignature only, critical mdlDS extended key
 * usage, AKI matching the IACA SKI, validity well under the 457-day maximum). See issue
 * #1891 — the previous hard-coded DS certificate was self-signed with CA extensions and
 * strict verifiers rejected it. Only the DS certificate goes into x5chain; verifiers under
 * test should be configured to trust the IACA root certificate.
 *
 * Parameterizing this key material via the test configuration is tracked in issue #1663.
 */
object TestKeysAndCerts {

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

	// mDL document signer OID, ISO/IEC 18013-5 Annex B: id-mdl-kp-mdlDS
	private const val MDL_DS_EKU_OID = "1.0.18013.5.1.2"

	@JvmStatic
	val iacaRootCert: X509Cert by lazy { X509Cert.fromPem(IACA_ROOT_CERT_PEM) }

	@JvmStatic
	val documentSignerKey: AsymmetricKey.X509CertifiedExplicit
		get() = currentDocumentSignerKey()

	@JvmStatic
	val documentSignerCert: X509Cert
		get() = documentSignerKey.certChain.certificates.first()

	private var cachedDocumentSignerKey: AsymmetricKey.X509CertifiedExplicit? = null
	private var cachedDocumentSignerKeyMintedAt: kotlin.time.Instant? = null

	// Re-minted daily (and so never served anywhere near its expiry): frequent rotation keeps
	// regeneration a routine event, so nothing can quietly start depending on one specific DS
	// certificate — the IACA root is the only stable thing to pin.
	@Synchronized
	private fun currentDocumentSignerKey(): AsymmetricKey.X509CertifiedExplicit {
		val cached = cachedDocumentSignerKey
		val mintedAt = cachedDocumentSignerKeyMintedAt
		if (cached != null && mintedAt != null && Clock.System.now() < mintedAt + 1.days) {
			return cached
		}
		return mintDocumentSignerKey().also {
			cachedDocumentSignerKey = it
			cachedDocumentSignerKeyMintedAt = Clock.System.now()
		}
	}

	private fun mintDocumentSignerKey(): AsymmetricKey.X509CertifiedExplicit {
		val iacaHolder = X509CertificateHolder(iacaRootCert.encoded.toByteArray())
		val iacaPrivateKey = KeyFactory.getInstance("EC").generatePrivate(
			PKCS8EncodedKeySpec(decodePemBody(IACA_ROOT_KEY_PEM))
		)

		val dsEcKey = ECKeyGenerator(Curve.P_256).generate()

		// uniform on [0, 2^159); max(ONE) only guards the astronomically improbable zero
		val serial = BigInteger(159, SecureRandom()).max(BigInteger.ONE)

		// Table B.3 subject rules: countryName mandatory and equal to the IACA's;
		// stateOrProvinceName mandatory (same value) because the IACA carries it; CN mandatory.
		val subject = X500NameBuilder(BCStyle.INSTANCE)
			.addRDN(BCStyle.C, "US")
			.addRDN(BCStyle.ST, "State of Utopia")
			.addRDN(BCStyle.L, "San Ramon")
			.addRDN(BCStyle.O, "OpenID Foundation")
			.addRDN(BCStyle.CN, "certification.openid.net mdoc DS")
			.build()

		val now = System.currentTimeMillis()
		val notBefore = Date(now - 5L * 60 * 1000)
		val notAfter = Date(now + 90L * 24 * 60 * 60 * 1000)

		val builder = X509v3CertificateBuilder(
			iacaHolder.subject,
			serial,
			notBefore,
			notAfter,
			subject,
			SubjectPublicKeyInfo.getInstance(dsEcKey.toECPublicKey().encoded)
		)

		val extensionUtils = JcaX509ExtensionUtils()
		val iacaSki = SubjectKeyIdentifier.fromExtensions(iacaHolder.extensions)
		builder.addExtension(
			Extension.subjectKeyIdentifier, false,
			extensionUtils.createSubjectKeyIdentifier(dsEcKey.toECPublicKey())
		)
		builder.addExtension(
			Extension.authorityKeyIdentifier, false,
			AuthorityKeyIdentifier(iacaSki.keyIdentifier)
		)
		builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature))
		builder.addExtension(
			Extension.extendedKeyUsage, true,
			ExtendedKeyUsage(KeyPurposeId.getInstance(ASN1ObjectIdentifier(MDL_DS_EKU_OID)))
		)
		builder.addExtension(
			Extension.issuerAlternativeName, false,
			GeneralNames(GeneralName(GeneralName.rfc822Name, "certification@oidf.org"))
		)
		// Placeholder URI carried over from the previous certificate; Table B.3 makes the
		// extension mandatory but the suite does not operate a CRL.
		builder.addExtension(
			Extension.cRLDistributionPoints, false,
			CRLDistPoint(
				arrayOf(
					DistributionPoint(
						DistributionPointName(
							GeneralNames(
								GeneralName(GeneralName.uniformResourceIdentifier, "http://example.com/myca.crl")
							)
						),
						null,
						null
					)
				)
			)
		)

		val signer = JcaContentSignerBuilder("SHA256withECDSA").build(iacaPrivateKey)
		val dsCert = X509Cert(ByteString(builder.build(signer).encoded))

		val dsPrivateKey = EcPrivateKeyDoubleCoordinate(
			EcCurve.P256,
			dsEcKey.d.decode(),
			dsEcKey.x.decode(),
			dsEcKey.y.decode()
		)
		return AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(dsCert)), dsPrivateKey)
	}

	private fun decodePemBody(pem: String): ByteArray {
		val body = pem.lines()
			.filterNot { it.startsWith("-----") }
			.joinToString("")
		return Base64.getMimeDecoder().decode(body)
	}
}
