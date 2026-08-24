package org.multipaz.testapp

import kotlinx.io.bytestring.ByteString
import com.nimbusds.jose.jwk.ECKey
import net.openid.conformance.util.TestKeysAndCerts
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

/**
 * Utility object for creating mdoc credentials in the VCI (Verifiable Credentials Issuance) context.
 * This creates an IssuerSigned structure (not a DeviceResponse like in VP flow).
 */
object VciMdocUtils {

	/**
	 * Creates an mdoc credential (IssuerSigned structure) for VCI issuance.
	 *
	 * @param devicePublicKeyJwk The device public key from the proof, as a JWK JSON string. Can be null for credentials without holder binding.
	 * @param docType The mdoc document type (e.g., "eu.europa.ec.eudi.pid.1")
	 * @param issuerSigningJwk Optional custom issuer signing key (JWK JSON string). If null, uses default test key.
	 * @param statusListUri Optional Token Status List URI to embed in the MSO (with statusListIndex). Used by
	 *   tests to exercise the status-reference checks; null (the default) means no status reference is included.
	 * @param statusListIndex Optional Token Status List index to embed in the MSO (with statusListUri).
	 * @return Base64URL-encoded IssuerSigned CBOR structure
	 */
	@JvmStatic
	@JvmOverloads
	fun createMdocCredential(
		devicePublicKeyJwk: String?,
		docType: String,
		issuerSigningJwk: String?,
		signedAtEpochSeconds: Long? = null,
		statusListUri: String? = null,
		statusListIndex: Long? = null
	): String {
		// Parse device public key from JWK (if provided)
		val devicePublicKey: EcPublicKey? = if (devicePublicKeyJwk != null) {
			convertJwkToEcPublicKey(JWK.parse(devicePublicKeyJwk))
		} else {
			null
		}

		// Use provided issuer key or default
		val dsKey: AsymmetricKey.X509Certified = if (issuerSigningJwk != null) {
			val issuerJwk = JWK.parse(issuerSigningJwk).toECKey()
			val privateKey = convertJwkToEcPrivateKey(issuerJwk)
			// VCIEnsureCredentialSigningCertificateIsNotSelfSigned enforces this at test start;
			// this backstop avoids silently pairing the configured key with the built-in
			// certificate, which would produce an unverifiable IssuerAuth signature.
			if (issuerJwk.x509CertChain.isNullOrEmpty()) {
				throw IllegalArgumentException(
					"The 'Signing JWK' field in the 'Credential Issuer' section of the test " +
						"configuration must contain the signing certificate chain in its 'x5c' claim"
				)
			}
			val chain = issuerJwk.x509CertChain.map { X509Cert(ByteString(it.decode())) }
			AsymmetricKey.X509CertifiedExplicit(X509CertChain(chain), privateKey)
		} else {
			TestKeysAndCerts.documentSignerKey
		}

		val now = Clock.System.now().truncateToWholeSeconds()
		// Round the issuance time down to the hour (or use an explicit value, e.g. in tests) so a
		// batch of credentials shares a coarse, low-entropy signed/validFrom rather than a precise
		// timestamp that lets verifiers link them (RFC 9901 §10.1).
		val signedAt = if (signedAtEpochSeconds != null) {
			Instant.fromEpochSeconds(signedAtEpochSeconds)
		} else {
			Instant.fromEpochSeconds(now.epochSeconds - (now.epochSeconds % 3600))
		}
		val validFrom = signedAt
		// Derive validUntil from the (rounded) signedAt, not the precise now, so that two same-dataset
		// credentials issued seconds apart share an identical, coarse validUntil rather than one that
		// differs by the inter-issuance gap (RFC 9901 §10.1) — matching the SD-JWT exp = iat + ttl fix.
		val validUntil = signedAt + 365.days

		// Build IssuerNamespaces based on docType
		// Keep the credential's issuing_country consistent with the signing certificate's
		// countryName (ISO 18013-5 Table B.3 binds the two), whichever key is in use.
		val issuingCountry = subjectCountry(dsKey.certChain.certificates.first()) ?: "US"
		val issuerNamespaces = buildIssuerNamespacesForDocType(docType, now, validUntil, issuingCountry)

		// Generate MSO (Mobile Security Object)
		// Note: For credentials without holder binding, devicePublicKey can be null
		// However, the multipaz library currently requires a device key for MSO generation
		if (devicePublicKey == null) {
			throw IllegalArgumentException(
				"mdoc credentials without cryptographic holder binding are not yet supported. " +
				"The MSO generator requires a device public key."
			)
		}
		val valueDigests = runBlocking { issuerNamespaces.getValueDigests(Algorithm.SHA256) }
		val revocationStatus = if (statusListUri != null && statusListIndex != null) {
			org.multipaz.revocation.RevocationStatus.StatusList(statusListIndex.toInt(), statusListUri, null)
		} else {
			null
		}
		val mso = MobileSecurityObject(
			version = "1.0",
			docType = docType,
			signedAt = signedAt,
			validFrom = validFrom,
			validUntil = validUntil,
			expectedUpdate = null,
			digestAlgorithm = Algorithm.SHA256,
			valueDigests = valueDigests,
			deviceKey = devicePublicKey,
			revocationStatus = revocationStatus,
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

	// ISO 18013-5 Table 5 makes portrait mandatory; a small generated JPEG keeps test credentials
	// small while still rendering as an obvious placeholder (silhouette + "OIDF" band) in wallets
	private val portraitJpeg: ByteArray by lazy {
		val width = 96
		val height = 128
		val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB)
		val g = image.createGraphics()
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
		g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
		g.paint = java.awt.GradientPaint(0f, 0f, java.awt.Color(0xDC, 0xE8, 0xF5), 0f, height.toFloat(), java.awt.Color(0xB8, 0xCC, 0xE0))
		g.fillRect(0, 0, width, height)
		g.color = java.awt.Color(0x4A, 0x62, 0x7E)
		g.fillOval(28, 18, 40, 44) // head
		g.fillRect(41, 54, 14, 14) // neck
		g.fillRoundRect(12, 66, 72, 70, 40, 40) // shoulders
		g.paint = java.awt.GradientPaint(0f, 66f, java.awt.Color(0xDC, 0xE8, 0xF5), 0f, 86f, java.awt.Color(0xC8, 0xD8, 0xEA))
		g.fillPolygon(intArrayOf(41, 48, 55), intArrayOf(66, 80, 66), 3) // collar notch
		g.color = java.awt.Color(0x1B, 0x32, 0x5C)
		g.fillRect(0, 102, width, 26)
		g.color = java.awt.Color.WHITE
		g.font = java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 16)
		g.drawString("OIDF", (width - g.fontMetrics.stringWidth("OIDF")) / 2, 121)
		g.dispose()
		val out = java.io.ByteArrayOutputStream()
		javax.imageio.ImageIO.write(image, "jpg", out)
		out.toByteArray()
	}

	private fun subjectCountry(cert: X509Cert): String? {
		val holder = org.bouncycastle.cert.X509CertificateHolder(cert.encoded.toByteArray())
		val rdns = holder.subject.getRDNs(org.bouncycastle.asn1.x500.style.BCStyle.C)
		return if (rdns.isEmpty()) null else org.bouncycastle.asn1.x500.style.IETFUtils.valueToString(rdns[0].first.value)
	}

	private fun buildIssuerNamespacesForDocType(
		docType: String,
		now: Instant,
		validUntil: Instant,
		issuingCountry: String
	) = buildIssuerNamespaces {
		when (docType) {
			"org.iso.18013.5.1.mDL" -> {
				// Mobile Driver's License (ISO 18013-5)
				addNamespace("org.iso.18013.5.1") {
					addDataElement("family_name", Tstr("Mustermann"))
					addDataElement("given_name", Tstr("Erika"))
					addDataElement("birth_date", Tagged(Tagged.FULL_DATE_STRING, Tstr("1985-03-15")))
					addDataElement("issue_date", Tagged(Tagged.FULL_DATE_STRING, Tstr(now.toString().substring(0, 10))))
					addDataElement("expiry_date", Tagged(Tagged.FULL_DATE_STRING, Tstr(validUntil.toString().substring(0, 10))))
					addDataElement("issuing_country", Tstr(issuingCountry))
					addDataElement("issuing_authority", Tstr("OpenID Foundation"))
					addDataElement("document_number", Tstr("DL-123456789"))
					addDataElement("portrait", Bstr(portraitJpeg))
					addDataElement("driving_privileges", buildCborArray {
						add(buildCborMap {
							put("vehicle_category_code", Tstr("B"))
							put("issue_date", Tagged(Tagged.FULL_DATE_STRING, Tstr("2010-01-01")))
							put("expiry_date", Tagged(Tagged.FULL_DATE_STRING, Tstr(validUntil.toString().substring(0, 10))))
						})
						add(buildCborMap {
							put("vehicle_category_code", Tstr("A"))
							put("issue_date", Tagged(Tagged.FULL_DATE_STRING, Tstr("2015-06-01")))
							put("expiry_date", Tagged(Tagged.FULL_DATE_STRING, Tstr(validUntil.toString().substring(0, 10))))
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
					addDataElement("birth_date", Tagged(Tagged.FULL_DATE_STRING, Tstr("1980-05-23")))
					addDataElement("age_in_years", Uint(44u))
					addDataElement("issuance_date", Tagged(Tagged.FULL_DATE_STRING, Tstr(now.toString().substring(0, 10))))
					addDataElement("expiry_date", Tagged(Tagged.FULL_DATE_STRING, Tstr(validUntil.toString().substring(0, 10))))
					addDataElement("issuing_authority", Tstr("OpenID Foundation Conformance Suite"))
					addDataElement("issuing_country", Tstr(issuingCountry))
				}
			}
			"net.openid.examples.certification.1.mdoc" -> {
				addNamespace("net.openid.examples.certification.1.mdoc") {
					addDataElement("product", Tstr("Some Product"))
					addDataElement("version", Tstr("1.2.3"))
					addDataElement("issuance_date", Tagged(Tagged.FULL_DATE_STRING, Tstr(now.toString().substring(0, 10))))
				}
			}
			else -> {
				// Default: use a generic namespace based on docType
				addNamespace(docType) {
					addDataElement("family_name", Tstr("Doe"))
					addDataElement("given_name", Tstr("John"))
					addDataElement("issuance_date", Tagged(Tagged.FULL_DATE_STRING, Tstr(now.toString().substring(0, 10))))
					addDataElement("expiry_date", Tagged(Tagged.FULL_DATE_STRING, Tstr(validUntil.toString().substring(0, 10))))
				}
			}
		}
	}

	private fun convertJwkToEcPublicKey(jwk: JWK): EcPublicKey {
		if (jwk is com.nimbusds.jose.jwk.OctetKeyPair) {
			// ISO 18013-5 also permits Curve25519/448 device keys, which JWK models as OKP
			val curve = when (jwk.curve) {
				com.nimbusds.jose.jwk.Curve.Ed25519 -> EcCurve.ED25519
				com.nimbusds.jose.jwk.Curve.Ed448 -> EcCurve.ED448
				com.nimbusds.jose.jwk.Curve.X25519 -> EcCurve.X25519
				com.nimbusds.jose.jwk.Curve.X448 -> EcCurve.X448
				else -> throw IllegalArgumentException("Unsupported OKP curve: " + jwk.curve)
			}
			return org.multipaz.crypto.EcPublicKeyOkp(curve, jwk.x.decode())
		}
		val ecKey = jwk.toECKey()
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
