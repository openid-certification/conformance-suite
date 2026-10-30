package net.openid.conformance.condition.client.lote

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.util.Base64
import com.nimbusds.jose.util.X509CertUtils
import net.openid.conformance.testmodule.Environment
import net.openid.conformance.util.LoteList
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date

/**
 * Builds lists of trusted entities (ETSI TS 119 602 JSON binding, compact JWS) for the LoTE
 * condition tests: a programmatic payload in the shape the EUDI reference lists have, signed with
 * a generated sealing certificate, plus the real EUDI development lists under test resources for
 * time-independent assertions.
 */
object LoteTestFixtures {

	const val PID_LIST_TYPE = "http://uri.etsi.org/19602/LoTEType/EUPIDProvidersList"
	const val WALLET_LIST_TYPE = "http://uri.etsi.org/19602/LoTEType/EUWalletProvidersList"
	const val WRPAC_LIST_TYPE = "http://uri.etsi.org/19602/LoTEType/EUWRPACProvidersList"
	const val PID_ISSUANCE = "http://uri.etsi.org/19602/SvcType/PID/Issuance"
	const val PID_REVOCATION = "http://uri.etsi.org/19602/SvcType/PID/Revocation"
	const val MDL_ISSUANCE = "http://trust.ec.europa.eu/lists/mDL/SvcType/Issuance"
	const val WALLET_ISSUANCE = "http://uri.etsi.org/19602/SvcType/WalletSolution/Issuance"
	const val WRPAC_ISSUANCE = "http://uri.etsi.org/19602/SvcType/WRPAC/Issuance"

	/** A sealing certificate and its key: the scheme operator's signer, self-signed. */
	class Signer(val key: ECKey, val cert: X509Certificate)

	/** A CA certificate (a trust anchor to list) with its key, so leaves can be minted under it. */
	class ListedCa(val keyPair: KeyPair, val cert: X509Certificate)

	fun generateSigner(country: String = "EU", organization: String = "LOTE User", notAfter: Instant? = null): Signer {
		val keyPair = ecKeyPair()
		val name = X500Name("C=$country, O=$organization, CN=LoTE sealing certificate")
		val now = Instant.now()
		val builder = JcaX509v3CertificateBuilder(
			name, randomSerial(), Date.from(now.minusSeconds(3600)),
			Date.from(notAfter ?: now.plusSeconds(365L * 24 * 3600)), name, keyPair.public
		)
		builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature))
		val cert = JcaX509CertificateConverter().getCertificate(
			builder.build(JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private))
		)
		val jwk = ECKey.Builder(Curve.P_256, keyPair.public as java.security.interfaces.ECPublicKey)
			.privateKey(keyPair.private)
			.algorithm(JWSAlgorithm.ES256)
			.x509CertChain(listOf(Base64.encode(cert.encoded)))
			.build()
		return Signer(jwk, cert)
	}

	/** A self-signed CA; [pathLen] 0 (the default) issues leaves only, 1 lets it issue a [generateSubCa]. */
	@JvmOverloads
	fun generateCa(commonName: String, organization: String = "Example Provider", pathLen: Int = 0): ListedCa {
		val keyPair = ecKeyPair()
		val name = X500Name("C=EU, O=$organization, CN=$commonName")
		val now = Instant.now()
		val builder = JcaX509v3CertificateBuilder(
			name, randomSerial(), Date.from(now.minusSeconds(3600)), Date.from(now.plusSeconds(5 * 365L * 24 * 3600)),
			name, keyPair.public
		)
		builder.addExtension(Extension.basicConstraints, true, BasicConstraints(pathLen))
		builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign))
		val cert = JcaX509CertificateConverter().getCertificate(
			builder.build(JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.private))
		)
		return ListedCa(keyPair, cert)
	}

	/** A subordinate CA issued by [root], so a list can carry both and a chain can end at either. */
	@JvmOverloads
	fun generateSubCa(root: ListedCa, commonName: String, organization: String = "Example Provider"): ListedCa {
		val keyPair = ecKeyPair()
		val now = Instant.now()
		val builder = JcaX509v3CertificateBuilder(
			X509CertificateHolder(root.cert.encoded).subject, randomSerial(), Date.from(now.minusSeconds(3600)),
			Date.from(now.plusSeconds(5 * 365L * 24 * 3600)), X500Name("C=EU, O=$organization, CN=$commonName"), keyPair.public
		)
		builder.addExtension(Extension.basicConstraints, true, BasicConstraints(0))
		builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign))
		val cert = JcaX509CertificateConverter().getCertificate(
			builder.build(JcaContentSignerBuilder("SHA256withECDSA").build(root.keyPair.private))
		)
		return ListedCa(keyPair, cert)
	}

	/** A leaf under [ca], as an entity listed in a LoTE would issue. */
	fun mintLeaf(ca: ListedCa, commonName: String): Pair<ECKey, X509Certificate> {
		val keyPair = ecKeyPair()
		val now = Instant.now()
		val builder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
			X509CertificateHolder(ca.cert.encoded).subject, randomSerial(),
			Date.from(now.minusSeconds(3600)), Date.from(now.plusSeconds(30L * 24 * 3600)),
			X500Name("C=EU, O=Example Provider, CN=$commonName"), keyPair.public
		)
		builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature))
		val cert = JcaX509CertificateConverter().getCertificate(
			builder.build(JcaContentSignerBuilder("SHA256withECDSA").build(ca.keyPair.private))
		)
		val jwk = ECKey.Builder(Curve.P_256, keyPair.public as java.security.interfaces.ECPublicKey)
			.privateKey(keyPair.private)
			.algorithm(JWSAlgorithm.ES256)
			.x509CertChain(listOf(Base64.encode(cert.encoded)))
			.build()
		return jwk to cert
	}

	/** One service entry in the shape the EUDI lists use. */
	fun service(serviceType: String, name: String, vararg certs: X509Certificate): JsonObject {
		val x509s = JsonArray()
		certs.forEach { c ->
			val entry = JsonObject()
			entry.addProperty("val", java.util.Base64.getEncoder().encodeToString(c.encoded))
			x509s.add(entry)
		}
		val identity = JsonObject()
		identity.add("X509Certificates", x509s)
		val info = JsonObject()
		info.addProperty("ServiceTypeIdentifier", serviceType)
		info.add("ServiceName", multilingual(name))
		info.add("ServiceDigitalIdentity", identity)
		info.add("SchemeServiceDefinitionURI", multilingualUri("https://example.com/service"))
		val wrapper = JsonObject()
		wrapper.add("ServiceInformation", info)
		return wrapper
	}

	fun trustedEntity(name: String, vararg services: JsonObject): JsonObject {
		val info = JsonObject()
		info.add("TEName", multilingual(name))
		info.add("TETradeName", multilingual("$name trade name"))
		val address = JsonObject()
		val postal = JsonArray()
		val pa = JsonObject()
		pa.addProperty("lang", "en")
		pa.addProperty("StreetAddress", "Street")
		pa.addProperty("Locality", "Town")
		pa.addProperty("PostalCode", "0000")
		pa.addProperty("Country", "EU")
		postal.add(pa)
		address.add("TEPostalAddress", postal)
		address.add("TEElectronicAddress", multilingualUri("mailto:te@example.com"))
		info.add("TEAddress", address)
		info.add("TEInformationURI", multilingualUri("https://example.com/te"))
		val svcs = JsonArray()
		services.forEach { svcs.add(it) }
		val entity = JsonObject()
		entity.add("TrustedEntityInformation", info)
		entity.add("TrustedEntityServices", svcs)
		return entity
	}

	/**
	 * A complete payload. [entities] null means an empty list (clause 6.4.0: no TrustedEntitiesList).
	 */
	fun payload(
		loteType: String,
		entities: List<JsonObject>?,
		issued: Instant = Instant.now().minusSeconds(60),
		nextUpdate: Instant? = Instant.now().plusSeconds(30L * 24 * 3600),
		version: Int = 1,
		territory: String = "EU",
		operator: String = "LOTE User",
		mutate: java.util.function.Consumer<JsonObject> = java.util.function.Consumer {}
	): JsonObject {
		val info = JsonObject()
		info.addProperty("LoTEVersionIdentifier", version)
		info.addProperty("LoTESequenceNumber", 7)
		info.add("SchemeOperatorName", multilingual(operator))
		// clause 6.1.3: second precision, as the real lists are issued
		info.addProperty("ListIssueDateTime", issued.truncatedTo(ChronoUnit.SECONDS).toString())
		if (nextUpdate != null) {
			info.addProperty("NextUpdate", nextUpdate.truncatedTo(ChronoUnit.SECONDS).toString())
		}
		info.addProperty("LoTEType", loteType)
		info.add("SchemeName", multilingual("EU Test List"))
		info.add("SchemeInformationURI", multilingualUri("https://example.com/lote"))
		// the profile of the list type fixes these; the PID list's values stand in for a type no profile covers
		val profile = LoteList.values().firstOrNull { it.loteType == loteType } ?: LoteList.PID_PROVIDERS
		info.addProperty("StatusDeterminationApproach", profile.statusDeterminationApproach)
		info.add("SchemeTypeCommunityRules", multilingualUri(profile.schemeTypeCommunityRules))
		info.addProperty("SchemeTerritory", territory)
		val dp = JsonArray()
		dp.add("https://example.com/lote.jwt")
		info.add("DistributionPoints", dp)
		val lote = JsonObject()
		lote.add("ListAndSchemeInformation", info)
		if (entities != null) {
			val list = JsonArray()
			entities.forEach { list.add(it) }
			lote.add("TrustedEntitiesList", list)
		}
		val root = JsonObject()
		root.add("LoTE", lote)
		mutate.accept(root)
		return root
	}

	/** Signs [payload] as the EUDI lists are signed: ES256, x5c, x5t#S256, typ JOSE+JSON, iat. */
	fun sign(payload: JsonObject, signer: Signer, includeX5c: Boolean = true, alg: JWSAlgorithm = JWSAlgorithm.ES256): String {
		val header = JWSHeader.Builder(alg)
			.type(JOSEObjectType("JOSE+JSON"))
			.customParam("iat", Instant.now().epochSecond)
		if (includeX5c) {
			header.x509CertChain(listOf(Base64.encode(signer.cert.encoded)))
				.x509CertSHA256Thumbprint(X509CertUtils.computeSHA256Thumbprint(signer.cert))
		}
		val jws = JWSObject(header.build(), Payload(payload.toString()))
		jws.sign(ECDSASigner(signer.key))
		return jws.serialize()
	}

	/** A signed PID providers list carrying [cas] under PID/Issuance (and PID/Revocation). */
	fun goodPidList(signer: Signer, vararg cas: X509Certificate): String =
		sign(payload(PID_LIST_TYPE, listOf(trustedEntity("EU PID Provider Trusted Entity",
			service(PID_ISSUANCE, "PID issuer", *cas), service(PID_REVOCATION, "PID revocation", *cas)))), signer)

	fun emptyPidList(signer: Signer): String = sign(payload(PID_LIST_TYPE, null), signer)

	fun eudiwDevList(name: String): String =
		LoteTestFixtures::class.java.getResourceAsStream("/lote/eudiw-dev-$name.jwt")!!.use { String(it.readAllBytes()).trim() }

	/** A time at which every checked-in Commission development list is current: after the latest ListIssueDateTime, before the earliest NextUpdate. */
	@JvmField
	val EC_DEVELOPMENT_LISTS_CURRENT: Clock = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC)

	fun ecDevelopmentList(name: String): String =
		LoteTestFixtures::class.java.getResourceAsStream("/lote/ec-development-$name.jwt")!!.use { String(it.readAllBytes()).trim() }

	/** Puts a fetched list into the environment as RegisterLote and CallLoteEndpoint would. */
	fun putLote(env: Environment, slot: LoteSlot, jws: String) {
		val lote = JsonObject()
		lote.addProperty("slot", slot.name)
		lote.addProperty("url", "https://lote.example.com/${slot.name.lowercase()}.jwt")
		lote.addProperty("value", jws)
		env.putObject(AbstractLoteCondition.LOTE, lote)
	}

	/** Puts a fetched and parsed list into the environment, as ParseLote leaves it. */
	fun putParsedLote(env: Environment, slot: LoteSlot, jws: String) {
		putLote(env, slot, jws)
		val parsed = JWSObject.parse(jws)
		env.putObject(AbstractLoteCondition.LOTE, "header", JsonParser.parseString(parsed.header.toString()).asJsonObject)
		env.putObject(AbstractLoteCondition.LOTE, "payload", JsonParser.parseString(parsed.payload.toString()).asJsonObject)
	}

	/** Puts extracted anchors into a slot's object, as ExtractLoteTrustAnchors leaves them, bypassing the list. */
	fun putAnchors(env: Environment, slot: LoteSlot, serviceType: String, teName: String, vararg certs: X509Certificate) {
		val existing = env.getElementFromObject(slot.envKey, "anchors.list")
		val list = if (existing != null && existing.isJsonArray) existing.asJsonArray else JsonArray()
		certs.forEach { c ->
			val anchor = JsonObject()
			anchor.addProperty("cert", java.util.Base64.getEncoder().encodeToString(c.encoded))
			anchor.addProperty("subject", c.subjectX500Principal.name)
			anchor.addProperty("service_type", serviceType)
			anchor.addProperty("te_name", teName)
			list.add(anchor)
		}
		val holder = JsonObject()
		holder.add("list", list)
		val lote = env.getObject(slot.envKey) ?: JsonObject().also { env.putObject(slot.envKey, it) }
		lote.addProperty("slot", slot.name)
		lote.add("anchors", holder)
	}

	private fun multilingual(value: String): JsonArray {
		val arr = JsonArray()
		val o = JsonObject()
		o.addProperty("lang", "en")
		o.addProperty("value", value)
		arr.add(o)
		return arr
	}

	private fun multilingualUri(uri: String): JsonArray {
		val arr = JsonArray()
		val o = JsonObject()
		o.addProperty("lang", "en")
		o.addProperty("uriValue", uri)
		arr.add(o)
		return arr
	}

	private fun ecKeyPair(): KeyPair =
		KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()

	private fun randomSerial(): BigInteger = BigInteger(127, SecureRandom()).max(BigInteger.ONE)
}
