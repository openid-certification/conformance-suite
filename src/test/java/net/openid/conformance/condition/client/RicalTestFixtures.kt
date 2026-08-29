package net.openid.conformance.condition.client

import com.google.gson.JsonObject
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import kotlinx.coroutines.runBlocking
import net.openid.conformance.testmodule.Environment
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Simple
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.Tstr
import org.multipaz.cbor.Uint
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.toDataItem
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseLabel
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.EcPrivateKey
import org.multipaz.crypto.EcPrivateKeyDoubleCoordinate
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.crypto.X509KeyUsage
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Builds ISO/IEC 18013-5 second edition draft Annex F signed RICAL test fixtures. The RICAL
 * CBOR is hand-assembled (rather than via multipaz's SignedRical.generate) so tests have full
 * control over every field, including deliberately invalid values.
 */
object RicalTestFixtures {

	const val READER_AUTHENTICATION_TYPE = ValidateRicalStructure.READER_AUTHENTICATION_RICAL_TYPE

	class RicalSigner(val key: EcPrivateKey, val cert: X509Cert)

	/** Generates a fresh P-256 RICAL signer key with a self-signed signer certificate. */
	@JvmStatic
	@JvmOverloads
	fun generateSigner(
		validFrom: Instant = Clock.System.now() - 1.days,
		validUntil: Instant = Clock.System.now() + 365.days
	): RicalSigner {
		val key = runBlocking { Crypto.createEcPrivateKey(EcCurve.P256) }
		val name = X500Name.fromName("CN=OIDF Test RICAL Signer,O=OpenID Foundation,C=UT")
		val cert = runBlocking {
			X509Cert.Builder(
				key.publicKey,
				AsymmetricKey.anonymous(key, Algorithm.ES256),
				ASN1Integer(1L),
				name,
				name,
				validFrom,
				validUntil
			).includeSubjectKeyIdentifier(true).build()
		}
		return RicalSigner(key, cert)
	}

	/** A test reader CA with a reader (end-entity) certificate issued by it. */
	class ReaderPki(val caCert: X509Cert, val readerKey: EcPrivateKey, val readerCert: X509Cert)

	/** Generates a test reader CA and a reader certificate signed by it. */
	@JvmStatic
	@JvmOverloads
	fun generateReaderPki(caCommonName: String = "OIDF Test Reader CA"): ReaderPki {
		val caKey = runBlocking { Crypto.createEcPrivateKey(EcCurve.P256) }
		val caName = X500Name.fromName("CN=$caCommonName,O=OpenID Foundation,C=UT")
		val caCert = runBlocking {
			X509Cert.Builder(
				caKey.publicKey,
				AsymmetricKey.anonymous(caKey, Algorithm.ES256),
				ASN1Integer(1L),
				caName,
				caName,
				Clock.System.now() - 1.days,
				Clock.System.now() + 365.days
			).includeSubjectKeyIdentifier(true)
				.setBasicConstraints(true, 0)
				.setKeyUsage(setOf(X509KeyUsage.KEY_CERT_SIGN, X509KeyUsage.CRL_SIGN))
				.build()
		}
		val readerKey = runBlocking { Crypto.createEcPrivateKey(EcCurve.P256) }
		val readerCert = runBlocking {
			X509Cert.Builder(
				readerKey.publicKey,
				AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(caCert)), caKey),
				ASN1Integer(2L),
				X500Name.fromName("CN=OIDF Test Reader,O=OpenID Foundation,C=UT"),
				caName,
				Clock.System.now() - 1.days,
				Clock.System.now() + 90.days
			).includeSubjectKeyIdentifier(true)
				.setAuthorityKeyIdentifierToCertificate(caCert)
				.setKeyUsage(setOf(X509KeyUsage.DIGITAL_SIGNATURE))
				.build()
		}
		return ReaderPki(caCert, readerKey, readerCert)
	}

	/** Builds a spec-complete CertificateInfo map for the given reader CA certificate. */
	@JvmStatic
	@JvmOverloads
	fun certificateInfo(
		cert: X509Cert,
		isTrustAnchor: Boolean = true,
		skiOverride: ByteArray? = null,
		omit: Set<String> = emptySet(),
		extraKeys: Map<String, DataItem> = emptyMap(),
		serialNumberNegativeBignumTag: Boolean = false
	): DataItem {
		val serialBytes = unsignedBigEndian(cert.serialNumber.value)
		// the mis-encoding seen on the Geneva 2026 interop RICAL: the serial encoded with
		// the negative-bignum tag instead of the unsigned-bignum tag
		val serialTag = if (serialNumberNegativeBignumTag) Tagged.NEGATIVE_BIGNUM else Tagged.UNSIGNED_BIGNUM
		return buildCborMap {
			if ("certificate" !in omit) put("certificate", Bstr(cert.encoded.toByteArray()))
			if ("serialNumber" !in omit) put("serialNumber", Tagged(serialTag, Bstr(serialBytes)))
			if ("ski" !in omit) put("ski", Bstr(skiOverride ?: cert.subjectKeyIdentifier!!))
			if ("isTrustAnchor" !in omit) {
				put("isTrustAnchor", if (isTrustAnchor) Simple.TRUE else Simple.FALSE)
			}
			extraKeys.forEach { (k, v) -> put(k, v) }
		}
	}

	/** Builds a RICAL payload map. Fields can be omitted or overridden for negative tests. */
	@JvmStatic
	@JvmOverloads
	fun buildRicalMap(
		certificateInfos: List<DataItem>,
		version: String = "1.0",
		provider: String = "OIDF Test RICAL Provider",
		date: Instant = Clock.System.now(),
		type: String? = READER_AUTHENTICATION_TYPE,
		nextUpdate: Instant? = Clock.System.now() + 30.days,
		id: Long? = 1L,
		omit: Set<String> = emptySet(),
		extraKeys: Map<String, DataItem> = emptyMap()
	): DataItem {
		return buildCborMap {
			if ("version" !in omit) put("version", Tstr(version))
			if ("provider" !in omit) put("provider", Tstr(provider))
			if ("date" !in omit) put("date", tdate(date))
			if (type != null && "type" !in omit) put("type", Tstr(type))
			if (nextUpdate != null && "nextUpdate" !in omit) put("nextUpdate", tdate(nextUpdate))
			if (id != null && "id" !in omit) put("id", Uint(id.toULong()))
			if ("certificateInfos" !in omit) {
				put("certificateInfos", buildCborArray { certificateInfos.forEach { add(it) } })
			}
			extraKeys.forEach { (k, v) -> put(k, v) }
		}
	}

	/** COSE_Sign1-signs the RICAL payload as per Annex F.3.2 (untagged, x5chain protected). */
	@JvmStatic
	@JvmOverloads
	fun sign(
		rical: DataItem,
		signer: RicalSigner = generateSigner(),
		x5chainInProtectedHeader: Boolean = true,
		includeX5chain: Boolean = true
	): ByteArray {
		val protectedHeaders = mutableMapOf<CoseLabel, DataItem>(
			CoseNumberLabel(Cose.COSE_LABEL_ALG) to Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()
		)
		val unprotectedHeaders = mutableMapOf<CoseLabel, DataItem>()
		if (includeX5chain) {
			val x5chain = X509CertChain(listOf(signer.cert)).toDataItem()
			if (x5chainInProtectedHeader) {
				protectedHeaders[CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN)] = x5chain
			} else {
				unprotectedHeaders[CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN)] = x5chain
			}
		}
		val coseSign1 = runBlocking {
			Cose.coseSign1Sign(
				AsymmetricKey.anonymous(signer.key, Algorithm.ES256),
				Cbor.encode(rical),
				true,
				protectedHeaders,
				unprotectedHeaders
			)
		}
		return Cbor.encode(coseSign1.toDataItem())
	}

	/** Convenience: a valid signed RICAL listing the given reader CA certificates. */
	@JvmStatic
	@JvmOverloads
	fun goodSignedRical(
		readerCaCerts: List<X509Cert>,
		signer: RicalSigner = generateSigner()
	): ByteArray {
		return sign(buildRicalMap(readerCaCerts.map { certificateInfo(it) }), signer)
	}

	/** Stores a signed RICAL in the environment the way RegisterRical / CallRicalEndpoint do. */
	@JvmStatic
	fun putRical(env: Environment, signedRical: ByteArray) {
		val rical = JsonObject()
		rical.addProperty("value", java.util.Base64.getEncoder().encodeToString(signedRical))
		env.putObject("rical", rical)
	}

	/**
	 * Builds a signed request object JWT with the reader certificate in the x5c header and
	 * stores it as the authorization_request_object, the way the VP verifier tests do.
	 */
	@JvmStatic
	fun putSignedRequestObject(env: Environment, pki: ReaderPki) {
		val readerKey = pki.readerKey as EcPrivateKeyDoubleCoordinate
		val nimbusKey = com.nimbusds.jose.jwk.ECKey.Builder(
			com.nimbusds.jose.jwk.Curve.P_256,
			com.nimbusds.jose.util.Base64URL.encode(readerKey.x),
			com.nimbusds.jose.util.Base64URL.encode(readerKey.y)
		)
			.d(com.nimbusds.jose.util.Base64URL.encode(readerKey.d))
			.build()
		val header = JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(JOSEObjectType("oauth-authz-req+jwt"))
			.x509CertChain(listOf(com.nimbusds.jose.util.Base64.encode(pki.readerCert.encoded.toByteArray())))
			.build()
		val claims = JWTClaimsSet.Builder()
			.claim("client_id", "x509_san_dns:verifier.example.com")
			.claim("response_type", "vp_token")
			.claim("nonce", "fixture-nonce")
			.build()
		val jwt = SignedJWT(header, claims)
		jwt.sign(ECDSASigner(nimbusKey))

		val requestObject = JsonObject()
		requestObject.addProperty("value", jwt.serialize())
		env.putObject("authorization_request_object", requestObject)
	}

	// Java-friendly Instant helpers for tests
	@JvmStatic
	fun now(): Instant = Clock.System.now()

	@JvmStatic
	fun soon(): Instant = Clock.System.now() + 30.days

	@JvmStatic
	fun past(): Instant = Clock.System.now() - 30.days

	private fun tdate(instant: Instant): DataItem =
		Tagged(Tagged.DATE_TIME_STRING, Tstr(instant.toString()))

	// DER integer content octets may carry a leading 0x00 sign byte; biguint must not
	private fun unsignedBigEndian(derInteger: ByteArray): ByteArray {
		var bytes = derInteger
		while (bytes.size > 1 && bytes[0] == 0.toByte()) {
			bytes = bytes.copyOfRange(1, bytes.size)
		}
		return bytes
	}
}
