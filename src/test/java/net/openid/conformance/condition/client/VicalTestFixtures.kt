package net.openid.conformance.condition.client

import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import net.openid.conformance.testmodule.Environment
import net.openid.conformance.util.MdocUtil
import kotlinx.io.bytestring.ByteString
import org.multipaz.asn1.ASN1
import org.multipaz.asn1.ASN1Integer
import org.multipaz.asn1.ASN1ObjectIdentifier
import org.multipaz.asn1.ASN1Sequence
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
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
import org.multipaz.crypto.X509KeyUsage
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Builds ISO/IEC 18013-5 Annex C signed VICAL test fixtures. The VICAL CBOR is
 * hand-assembled (rather than via multipaz's SignedVical.generate) so tests have
 * full control over every field, including deliberately invalid values.
 */
object VicalTestFixtures {

	// reference the production constant so the fixture and the condition cannot drift
	val MDL_VICAL_SIGNER_EKU_OID: String = ValidateVicalSignerCertificateProfile.MDL_VICAL_SIGNER_EKU_OID

	class VicalSigner(val key: EcPrivateKey, val cert: X509Cert) {
		val asymmetricKey: AsymmetricKey
			get() = AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(cert)), key)
	}

	/** Generates a fresh P-256 VICAL signer key with a self-signed signer certificate. */
	@JvmStatic
	@JvmOverloads
	fun generateSigner(
		includeEku: Boolean = true,
		validFrom: Instant = Clock.System.now() - 1.days,
		validUntil: Instant = Clock.System.now() + 365.days
	): VicalSigner {
		val key = runBlocking { Crypto.createEcPrivateKey(EcCurve.P256) }
		val name = X500Name.fromName("CN=OIDF Test VICAL Signer,O=OpenID Foundation,C=UT")
		val builder = X509Cert.Builder(
			key.publicKey,
			AsymmetricKey.anonymous(key, Algorithm.ES256),
			ASN1Integer(1L),
			name,
			name,
			validFrom,
			validUntil
		).includeSubjectKeyIdentifier(true)
		if (includeEku) {
			// ExtKeyUsageSyntax ::= SEQUENCE OF KeyPurposeId; id-mdl-kp-mdlVICAL per Annex C.1.7.2
			builder.addExtension(
				"2.5.29.37",
				true,
				ASN1.encode(ASN1Sequence(listOf(ASN1ObjectIdentifier(MDL_VICAL_SIGNER_EKU_OID))))
			)
		}
		val cert = runBlocking { builder.build() }
		return VicalSigner(key, cert)
	}

	/** Builds a spec-complete CertificateInfo map for the given IACA certificate. */
	@JvmStatic
	@JvmOverloads
	fun certificateInfo(
		cert: X509Cert,
		docTypes: List<String> = listOf("org.iso.18013.5.1.mDL"),
		skiOverride: ByteArray? = null,
		serialNumberOverride: ByteArray? = null,
		omit: Set<String> = emptySet(),
		extraKeys: Map<String, DataItem> = emptyMap()
	): DataItem {
		val serialBytes = serialNumberOverride ?: unsignedBigEndian(cert.serialNumber.value)
		return buildCborMap {
			if ("certificate" !in omit) put("certificate", Bstr(cert.encoded.toByteArray()))
			if ("serialNumber" !in omit) put("serialNumber", Tagged(Tagged.UNSIGNED_BIGNUM, Bstr(serialBytes)))
			if ("ski" !in omit) put("ski", Bstr(skiOverride ?: cert.subjectKeyIdentifier!!))
			if ("docType" !in omit) {
				put("docType", buildCborArray { docTypes.forEach { add(Tstr(it)) } })
			}
			extraKeys.forEach { (k, v) -> put(k, v) }
		}
	}

	/** Builds a VICAL payload map. Fields can be omitted or overridden for negative tests. */
	@JvmStatic
	@JvmOverloads
	fun buildVicalMap(
		certificateInfos: List<DataItem>,
		version: String = "1.0",
		provider: String = "OIDF Test VICAL Provider",
		date: Instant = Clock.System.now(),
		nextUpdate: Instant? = Clock.System.now() + 30.days,
		vicalIssueID: Long? = 1L,
		omit: Set<String> = emptySet(),
		extraKeys: Map<String, DataItem> = emptyMap()
	): DataItem {
		return buildCborMap {
			if ("version" !in omit) put("version", Tstr(version))
			if ("vicalProvider" !in omit) put("vicalProvider", Tstr(provider))
			if ("date" !in omit) put("date", tdate(date))
			if (vicalIssueID != null && "vicalIssueID" !in omit) put("vicalIssueID", Uint(vicalIssueID.toULong()))
			if (nextUpdate != null && "nextUpdate" !in omit) put("nextUpdate", tdate(nextUpdate))
			if ("certificateInfos" !in omit) {
				put("certificateInfos", buildCborArray { certificateInfos.forEach { add(it) } })
			}
			extraKeys.forEach { (k, v) -> put(k, v) }
		}
	}

	/** COSE_Sign1-signs the VICAL payload as per Annex C.1.7.1 (untagged, x5chain unprotected). */
	@JvmStatic
	@JvmOverloads
	fun sign(
		vical: DataItem,
		signer: VicalSigner = generateSigner(),
		includeX5chain: Boolean = true,
		extraProtectedHeaders: Map<CoseLabel, DataItem> = emptyMap()
	): ByteArray {
		val protectedHeaders = mutableMapOf<CoseLabel, DataItem>(
			CoseNumberLabel(Cose.COSE_LABEL_ALG) to Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()
		)
		protectedHeaders.putAll(extraProtectedHeaders)
		val unprotectedHeaders = if (includeX5chain) {
			mapOf<CoseLabel, DataItem>(
				CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN) to X509CertChain(listOf(signer.cert)).toDataItem()
			)
		} else {
			emptyMap()
		}
		val coseSign1 = runBlocking {
			Cose.coseSign1Sign(
				AsymmetricKey.anonymous(signer.key, Algorithm.ES256),
				Cbor.encode(vical),
				true,
				protectedHeaders,
				unprotectedHeaders
			)
		}
		return Cbor.encode(coseSign1.toDataItem())
	}

	/** Convenience: a valid signed VICAL listing the given IACA certificates. */
	@JvmStatic
	@JvmOverloads
	fun goodSignedVical(
		iacaCerts: List<X509Cert>,
		docTypes: List<String> = listOf("org.iso.18013.5.1.mDL"),
		signer: VicalSigner = generateSigner()
	): ByteArray {
		return sign(buildVicalMap(iacaCerts.map { certificateInfo(it, docTypes) }), signer)
	}

	/** Extracts the issuerAuth signing certificate (x5chain leaf) from an IssuerSigned structure. */
	@JvmStatic
	fun signingCertFromIssuerSigned(issuerSignedBytes: ByteArray): X509Cert =
		MdocUtil.extractX5chain(Cbor.decode(issuerSignedBytes)).certificates[0]

	/** Stores a signed VICAL in the environment the way RegisterVical / CallVicalEndpoint do. */
	@JvmStatic
	fun putVical(env: Environment, signedVical: ByteArray) {
		val vical = JsonObject()
		vical.addProperty("value", java.util.Base64.getEncoder().encodeToString(signedVical))
		env.putObject("vical", vical)
	}

	/** A test IACA root with a document signer certificate issued by it. */
	class IssuerPki(val iacaCert: X509Cert, val dsKey: EcPrivateKey, val dsCert: X509Cert)

	/** Generates a test IACA root CA and a DS certificate signed by it. */
	@JvmStatic
	fun generateIssuerPki(): IssuerPki {
		val iacaKey = runBlocking { Crypto.createEcPrivateKey(EcCurve.P256) }
		val iacaName = X500Name.fromName("CN=OIDF Test IACA,O=OpenID Foundation,C=UT")
		val iacaCert = runBlocking {
			X509Cert.Builder(
				iacaKey.publicKey,
				AsymmetricKey.anonymous(iacaKey, Algorithm.ES256),
				ASN1Integer(1L),
				iacaName,
				iacaName,
				Clock.System.now() - 1.days,
				Clock.System.now() + 365.days
			).includeSubjectKeyIdentifier(true)
				.setBasicConstraints(true, 0)
				.setKeyUsage(setOf(X509KeyUsage.KEY_CERT_SIGN, X509KeyUsage.CRL_SIGN))
				.build()
		}
		val dsKey = runBlocking { Crypto.createEcPrivateKey(EcCurve.P256) }
		val dsCert = runBlocking {
			X509Cert.Builder(
				dsKey.publicKey,
				AsymmetricKey.X509CertifiedExplicit(X509CertChain(listOf(iacaCert)), iacaKey),
				ASN1Integer(2L),
				X500Name.fromName("CN=OIDF Test DS,O=OpenID Foundation,C=UT"),
				iacaName,
				Clock.System.now() - 1.days,
				Clock.System.now() + 90.days
			).includeSubjectKeyIdentifier(true)
				.setAuthorityKeyIdentifierToCertificate(iacaCert)
				.setKeyUsage(setOf(X509KeyUsage.DIGITAL_SIGNATURE))
				.build()
		}
		return IssuerPki(iacaCert, dsKey, dsCert)
	}

	/** PEM-encodes a certificate. */
	@JvmStatic
	fun toPem(cert: X509Cert): String =
		"-----BEGIN CERTIFICATE-----\n" +
			java.util.Base64.getMimeEncoder(64, "\n".toByteArray())
				.encodeToString(cert.encoded.toByteArray()) +
			"\n-----END CERTIFICATE-----\n"

	/** Creates an IssuerSigned mdoc whose issuerAuth is signed by the given PKI's DS key/cert. */
	@JvmStatic
	fun issuerSignedFromPki(pki: IssuerPki, docType: String): ByteArray {
		val dsKey = pki.dsKey as EcPrivateKeyDoubleCoordinate
		val issuerJwk = com.nimbusds.jose.jwk.ECKey.Builder(
			com.nimbusds.jose.jwk.Curve.P_256,
			com.nimbusds.jose.util.Base64URL.encode(dsKey.x),
			com.nimbusds.jose.util.Base64URL.encode(dsKey.y)
		)
			.d(com.nimbusds.jose.util.Base64URL.encode(dsKey.d))
			.x509CertChain(listOf(com.nimbusds.jose.util.Base64.encode(pki.dsCert.encoded.toByteArray())))
			.build()
		val deviceJwk = com.nimbusds.jose.jwk.gen.ECKeyGenerator(com.nimbusds.jose.jwk.Curve.P_256).generate()
		val mdocBase64Url = org.multipaz.testapp.VciMdocUtils.createMdocCredential(
			deviceJwk.toPublicJWK().toJSONString(), docType, issuerJwk.toJSONString())
		return com.nimbusds.jose.util.Base64URL(mdocBase64Url).decode()
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
