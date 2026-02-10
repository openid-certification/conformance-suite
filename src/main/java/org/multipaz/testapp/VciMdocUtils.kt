package org.multipaz.testapp

import kotlinx.io.bytestring.ByteString
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.runBlocking
import org.multipaz.cbor.*
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseLabel
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.crypto.*
import org.multipaz.mdoc.issuersigned.buildIssuerNamespaces
import org.multipaz.mdoc.mso.MobileSecurityObject
import org.multipaz.util.truncateToWholeSeconds
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Utility object for creating mdoc credentials in the VCI (Verifiable Credentials Issuance) context.
 * This creates an IssuerSigned structure (not a DeviceResponse like in VP flow).
 */
object VciMdocUtils {

	private val documentSignerKeyPub = EcPublicKey.fromPem(
		"""-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEnmiWAMGIeo2E3usWRLL/EPfh1Bw5
JHgq8RYzJvraMj5QZSh94CL/nlEi3vikGxDP34HjxZcjzGEimGg03sB6Ng==
-----END PUBLIC KEY-----""",
		EcCurve.P256
	)

	private val documentSignerKey = EcPrivateKey.fromPem(
		"""-----BEGIN PRIVATE KEY-----
MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg/ANvinTxJAdR8nQ0
NoUdBMcRJz+xLsb0kmhyMk+lkkGhRANCAASeaJYAwYh6jYTe6xZEsv8Q9+HUHDkk
eCrxFjMm+toyPlBlKH3gIv+eUSLe+KQbEM/fgePFlyPMYSKYaDTewHo2
-----END PRIVATE KEY-----""",
		documentSignerKeyPub
	)

	private val documentSignerCert = X509Cert.fromPem(
		"""-----BEGIN CERTIFICATE-----
MIICqTCCAlCgAwIBAgIUEmctHgzxSGqk6Z8Eb+0s97VZdpowCgYIKoZIzj0EAwIw
gYcxCzAJBgNVBAYTAlVTMRgwFgYDVQQIDA9TdGF0ZSBvZiBVdG9waWExEjAQBgNV
BAcMCVNhbiBSYW1vbjEaMBgGA1UECgwRT3BlbklEIEZvdW5kYXRpb24xCzAJBgNV
BAsMAklUMSEwHwYDVQQDDBhjZXJ0aWZpY2F0aW9uLm9wZW5pZC5uZXQwHhcNMjUw
NzMwMDc0NzIyWhcNMjYwNzMwMDc0NzIyWjCBhzELMAkGA1UEBhMCVVMxGDAWBgNV
BAgMD1N0YXRlIG9mIFV0b3BpYTESMBAGA1UEBwwJU2FuIFJhbW9uMRowGAYDVQQK
DBFPcGVuSUQgRm91bmRhdGlvbjELMAkGA1UECwwCSVQxITAfBgNVBAMMGGNlcnRp
ZmljYXRpb24ub3BlbmlkLm5ldDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJ5o
lgDBiHqNhN7rFkSy/xD34dQcOSR4KvEWMyb62jI+UGUofeAi/55RIt74pBsQz9+B
48WXI8xhIphoNN7AejajgZcwgZQwEgYDVR0TAQH/BAgwBgEB/wIBADAOBgNVHQ8B
Af8EBAMCAQYwIQYDVR0SBBowGIEWY2VydGlmaWNhdGlvbkBvaWRmLm9yZzAsBgNV
HR8EJTAjMCGgH6AdhhtodHRwOi8vZXhhbXBsZS5jb20vbXljYS5jcmwwHQYDVR0O
BBYEFHhk9LVVH8Gt9ZgfxgyhSl921XOhMAoGCCqGSM49BAMCA0cAMEQCICBxjCq9
efAwMKREK+k0OXBtiQCbFD7QdpyH42LVYfdvAiAurlZwp9PtmQZzoSYDUvXpZM5v
TvFLVc4ESGy3AtdC+g==
-----END CERTIFICATE-----"""
	)

	/**
	 * Creates an mdoc credential (IssuerSigned structure) for VCI issuance.
	 *
	 * @param devicePublicKeyJwk The device public key from the proof, as a JWK JSON string. Can be null for credentials without holder binding.
	 * @param docType The mdoc document type (e.g., "eu.europa.ec.eudi.pid.1")
	 * @param issuerSigningJwk Optional custom issuer signing key (JWK JSON string). If null, uses default test key.
	 * @return Base64URL-encoded IssuerSigned CBOR structure
	 */
	@JvmStatic
	fun createMdocCredential(
		devicePublicKeyJwk: String?,
		docType: String,
		issuerSigningJwk: String?
	): String {
		// Parse device public key from JWK (if provided)
		val devicePublicKey: EcPublicKey? = if (devicePublicKeyJwk != null) {
			val jwk = JWK.parse(devicePublicKeyJwk)
			val ecKey = jwk.toECKey()
			convertJwkToEcPublicKey(ecKey)
		} else {
			null
		}

		// Use provided issuer key or default
		val dsKey: AsymmetricKey.X509Certified = if (issuerSigningJwk != null) {
			val issuerJwk = JWK.parse(issuerSigningJwk).toECKey()
			val privateKey = convertJwkToEcPrivateKey(issuerJwk)
			val cert = if (issuerJwk.x509CertChain != null && issuerJwk.x509CertChain.isNotEmpty()) {
				X509Cert(ByteString(issuerJwk.x509CertChain[0].decode()))
			} else {
				documentSignerCert
			}
			AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(cert)), privateKey)
		} else {
			AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(documentSignerCert)), documentSignerKey)
		}

		val now = Clock.System.now().truncateToWholeSeconds()
		val signedAt = now - 1.hours
		val validFrom = now - 1.hours
		val validUntil = now + 365.days

		// Build IssuerNamespaces based on docType
		val issuerNamespaces = buildIssuerNamespacesForDocType(docType, now, validUntil)

		// Generate MSO (Mobile Security Object)
		// Note: For credentials without holder binding, devicePublicKey can be null
		// However, the multipaz library currently requires a device key for MSO generation
		if (devicePublicKey == null) {
			throw IllegalArgumentException(
				"mdoc credentials without cryptographic holder binding are not yet supported. " +
				"The MSO generator requires a device public key."
			)
		}
		val mso = MobileSecurityObject(
			version = "1.0",
			docType = docType,
			signedAt = signedAt,
			validFrom = validFrom,
			validUntil = validUntil,
			expectedUpdate = null,
			digestAlgorithm = Algorithm.SHA256,
			valueDigests = issuerNamespaces.getValueDigests(Algorithm.SHA256),
			deviceKey = devicePublicKey,
		)
		val taggedEncodedMso = Cbor.encode(Tagged(Tagged.ENCODED_CBOR, Bstr(Cbor.encode(mso.toDataItem()))))

		// Create COSE_Sign1 for IssuerAuth
		val protectedHeaders = mapOf<CoseLabel, org.multipaz.cbor.DataItem>(
			Pair(
				CoseNumberLabel(Cose.COSE_LABEL_ALG),
				Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()
			)
		)
		val unprotectedHeaders = mapOf<CoseLabel, org.multipaz.cbor.DataItem>(
			Pair(
				CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN),
				dsKey.certChain.toDataItem()
			)
		)
		val encodedIssuerAuth = Cbor.encode(
			runBlocking {
				Cose.coseSign1Sign(
					dsKey,
					taggedEncodedMso,
					true,
					protectedHeaders,
					unprotectedHeaders
				)
			}.toDataItem()
		)

		// Build IssuerSigned structure
		val issuerSigned = Cbor.encode(
			buildCborMap {
				put("nameSpaces", issuerNamespaces.toDataItem())
				put("issuerAuth", RawCbor(encodedIssuerAuth))
			}
		)

		return Base64URL.encode(issuerSigned).toString()
	}

	private fun buildIssuerNamespacesForDocType(
		docType: String,
		now: Instant,
		validUntil: Instant
	) = buildIssuerNamespaces {
		when (docType) {
			"org.iso.18013.5.1.mDL" -> {
				// Mobile Driver's License (ISO 18013-5)
				addNamespace("org.iso.18013.5.1") {
					addDataElement("family_name", Tstr("Mustermann"))
					addDataElement("given_name", Tstr("Erika"))
					addDataElement("birth_date", Tstr("1985-03-15"))
					addDataElement("issue_date", Tstr(now.toString().substring(0, 10)))
					addDataElement("expiry_date", Tstr(validUntil.toString().substring(0, 10)))
					addDataElement("issuing_country", Tstr("UT")) // Utopia
					addDataElement("issuing_authority", Tstr("OpenID Foundation"))
					addDataElement("document_number", Tstr("DL-123456789"))
					addDataElement("driving_privileges", buildCborArray {
						add(buildCborMap {
							put("vehicle_category_code", Tstr("B"))
							put("issue_date", Tstr("2010-01-01"))
							put("expiry_date", Tstr(validUntil.toString().substring(0, 10)))
						})
						add(buildCborMap {
							put("vehicle_category_code", Tstr("A"))
							put("issue_date", Tstr("2015-06-01"))
							put("expiry_date", Tstr(validUntil.toString().substring(0, 10)))
						})
					})
					addDataElement("un_distinguishing_sign", Tstr("UT"))
				}
			}
			"eu.europa.ec.eudi.pid.1" -> {
				// EU Personal ID
				addNamespace("eu.europa.ec.eudi.pid.1") {
					addDataElement("family_name", Tstr("Dupont"))
					addDataElement("given_name", Tstr("Jean"))
					addDataElement("birth_date", Tstr("1980-05-23"))
					addDataElement("age_in_years", Uint(44u))
					addDataElement("issuance_date", Tstr(now.toString().substring(0, 10)))
					addDataElement("expiry_date", Tstr(validUntil.toString().substring(0, 10)))
					addDataElement("issuing_authority", Tstr("OpenID Foundation Conformance Suite"))
					addDataElement("issuing_country", Tstr("UT")) // Utopia
				}
			}
			else -> {
				// Default: use a generic namespace based on docType
				addNamespace(docType) {
					addDataElement("family_name", Tstr("Doe"))
					addDataElement("given_name", Tstr("John"))
					addDataElement("issuance_date", Tstr(now.toString().substring(0, 10)))
					addDataElement("expiry_date", Tstr(validUntil.toString().substring(0, 10)))
				}
			}
		}
	}

	private fun convertJwkToEcPublicKey(ecKey: ECKey): EcPublicKey {
		val x = ecKey.x.decode()
		val y = ecKey.y.decode()
		return EcPublicKeyDoubleCoordinate(EcCurve.P256, x, y)
	}

	private fun convertJwkToEcPrivateKey(ecKey: ECKey): EcPrivateKey {
		val d = ecKey.d.decode()
		val x = ecKey.x.decode()
		val y = ecKey.y.decode()
		return org.multipaz.crypto.EcPrivateKeyDoubleCoordinate(EcCurve.P256, d, x, y)
	}
}
