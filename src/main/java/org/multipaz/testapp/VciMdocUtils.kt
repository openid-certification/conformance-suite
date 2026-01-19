package org.multipaz.testapp

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.util.Base64URL
import kotlinx.datetime.Clock
import org.multipaz.cbor.*
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseLabel
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.crypto.*
import org.multipaz.mdoc.issuersigned.buildIssuerNamespaces
import org.multipaz.mdoc.mso.MobileSecurityObjectGenerator
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
	 * @param devicePublicKeyJwk The device public key from the proof, as a JWK JSON string
	 * @param docType The mdoc document type (e.g., "eu.europa.ec.eudi.pid.1")
	 * @param issuerSigningJwk Optional custom issuer signing key (JWK JSON string). If null, uses default test key.
	 * @return Base64URL-encoded IssuerSigned CBOR structure
	 */
	@JvmStatic
	fun createMdocCredential(
		devicePublicKeyJwk: String,
		docType: String,
		issuerSigningJwk: String?
	): String {
		// Parse device public key from JWK
		val jwk = JWK.parse(devicePublicKeyJwk)
		val ecKey = jwk.toECKey()
		val devicePublicKey = convertJwkToEcPublicKey(ecKey)

		// Use provided issuer key or default
		val (dsKey, dsCert) = if (issuerSigningJwk != null) {
			val issuerJwk = JWK.parse(issuerSigningJwk).toECKey()
			val privateKey = convertJwkToEcPrivateKey(issuerJwk)
			// For custom keys, we'd need a certificate - for now use default cert
			// In production, the certificate should be provided with the key
			Pair(privateKey, documentSignerCert)
		} else {
			Pair(documentSignerKey, documentSignerCert)
		}

		val now = Clock.System.now()
		val signedAt = now - 1.hours
		val validFrom = now - 1.hours
		val validUntil = now + 365.days

		// Build IssuerNamespaces with sample PID data
		val issuerNamespaces = buildIssuerNamespaces {
			addNamespace("eu.europa.ec.eudi.pid.1") {
				addDataElement("family_name", Tstr("Dupont"))
				addDataElement("given_name", Tstr("Jean"))
				addDataElement("birth_date", Tstr("1980-05-23"))
				addDataElement("age_in_years", org.multipaz.cbor.Uint(44u))
				addDataElement("issuance_date", Tstr(now.toString().substring(0, 10)))
				addDataElement("expiry_date", Tstr(validUntil.toString().substring(0, 10)))
				addDataElement("issuing_authority", Tstr("OpenID Foundation Conformance Suite"))
				addDataElement("issuing_country", Tstr("UT")) // Utopia
			}
		}

		// Generate MSO (Mobile Security Object)
		val msoGenerator = MobileSecurityObjectGenerator(
			Algorithm.SHA256,
			docType,
			devicePublicKey
		)
		msoGenerator.setValidityInfo(signedAt, validFrom, validUntil, null)
		msoGenerator.addValueDigests(issuerNamespaces)

		val mso = msoGenerator.generate()
		val taggedEncodedMso = Cbor.encode(Tagged(24, Bstr(mso)))

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
				X509CertChain(listOf(dsCert)).toDataItem()
			)
		)
		val encodedIssuerAuth = Cbor.encode(
			Cose.coseSign1Sign(
				dsKey,
				taggedEncodedMso,
				true,
				dsKey.publicKey.curve.defaultSigningAlgorithm,
				protectedHeaders,
				unprotectedHeaders
			).toDataItem()
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
