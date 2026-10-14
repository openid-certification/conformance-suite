package net.openid.conformance.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.IssuerSerial;
import org.bouncycastle.cert.X509CertificateHolder;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Builds the lists of trusted entities (ETSI TS 119 602, JSON binding of Annex A.1) the suite
 * publishes about itself: one per {@link LoteList}, carrying the suite CAs as trust anchors under
 * the service types the EU profiles define, with the scheme information the profiles require
 * (Annex D.2 and its siblings) and the suite as scheme operator. The list is sealed as the EUDI
 * reference lists are: a compact JWS, ES256, with the sealing certificate in {@code x5c} and its
 * thumbprint in {@code x5t#S256}, a {@code kid} naming the certificate's issuer and serial, an
 * {@code iat} and {@code typ} {@code JOSE+JSON}. That {@code typ} is what the reference lists
 * carry, not the value RFC 7515 defines for compact serialization; the suite matches them so a
 * consumer built against them reads this list too.
 *
 * <p>A list is issued for 24 hours and its sequence number is the issue time in epoch seconds,
 * so it increases across restarts with no shared state; the profiles only cap the interval at
 * six months. An empty variant of each list, without {@code TrustedEntitiesList} (clause 6.4.0),
 * serves the negative tests that need a list trusting nobody.
 */
public final class LoteBuilder {

	/** The path prefix the lists are served under: {@code /lote/{slug}.jwt}; empty ones under {@code /lote/empty/}. */
	public static final String PATH_PREFIX = "/lote/";
	public static final String EMPTY_PATH_PREFIX = "/lote/empty/";

	public static final String SCHEME_OPERATOR_NAME = TestKeysAndCerts.ORGANIZATION_NAME;
	public static final String TRUSTED_ENTITY_NAME = TestKeysAndCerts.ORGANIZATION_NAME;
	public static final String SCHEME_TERRITORY = "US";

	private static final Duration VALIDITY = Duration.ofHours(24);

	private LoteBuilder() {
	}

	/** The URL the suite serves {@code list} at, under its https base URL. */
	public static String url(LoteList list, boolean empty) {
		return TestKeysAndCerts.getHttpsBaseUrl() + (empty ? EMPTY_PATH_PREFIX : PATH_PREFIX) + list.slug + ".jwt";
	}

	/** The signed list issued now. */
	public static String build(LoteList list, boolean empty) {
		return build(list, empty, Instant.now());
	}

	/** The signed list issued at {@code issued}. */
	public static String build(LoteList list, boolean empty, Instant issued) {
		JsonObject payload = payload(list, empty, issued);
		try {
			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
				.type(new JOSEObjectType("JOSE+JSON"))
				.keyID(issuerSerialKeyId())
				.x509CertChain(TestKeysAndCerts.getLoteSignerJwk().getX509CertChain())
				.x509CertSHA256Thumbprint(X509CertUtils.computeSHA256Thumbprint(TestKeysAndCerts.getLoteSignerCert()))
				.customParam("iat", issued.getEpochSecond())
				.build();
			JWSObject jws = new JWSObject(header, new Payload(payload.toString()));
			jws.sign(new ECDSASigner(TestKeysAndCerts.getLoteSignerJwk()));
			return jws.serialize();
		} catch (JOSEException e) {
			throw new IllegalStateException("Failed to seal the " + list.slug + " list", e);
		}
	}

	/** The JSON payload (the {@code LoTE} object and its wrapper) of {@code list} issued at {@code issued}. */
	public static JsonObject payload(LoteList list, boolean empty, Instant issued) {
		Instant truncated = issued.truncatedTo(ChronoUnit.SECONDS);
		String url = url(list, empty);

		JsonObject info = new JsonObject();
		info.addProperty("LoTEVersionIdentifier", 1);
		info.addProperty("LoTESequenceNumber", truncated.getEpochSecond());
		info.add("SchemeOperatorName", multilingual(SCHEME_OPERATOR_NAME));
		info.addProperty("ListIssueDateTime", DateTimeFormatter.ISO_INSTANT.format(truncated));
		info.addProperty("NextUpdate", DateTimeFormatter.ISO_INSTANT.format(truncated.plus(VALIDITY)));
		info.addProperty("LoTEType", list.loteType);
		JsonObject address = new JsonObject();
		address.add("SchemeOperatorElectronicAddress", multilingualUri("mailto:certification@oidf.org"));
		info.add("SchemeOperatorAddress", address);
		info.add("SchemeName", multilingual(list.schemeName + (empty ? " (empty)" : "")));
		info.add("SchemeInformationURI", multilingualUri(TestKeysAndCerts.getHttpsBaseUrl() + "/trust-anchors"));
		info.addProperty("StatusDeterminationApproach", list.statusDeterminationApproach);
		info.add("SchemeTypeCommunityRules", multilingualUri(list.schemeTypeCommunityRules));
		info.addProperty("SchemeTerritory", SCHEME_TERRITORY);
		JsonArray policies = new JsonArray();
		JsonObject policy = new JsonObject();
		JsonObject policyUri = new JsonObject();
		policyUri.addProperty("lang", "en");
		policyUri.addProperty("uriValue", "https://openid.net/certification/");
		policy.add("LoTEPolicy", policyUri);
		policies.add(policy);
		info.add("PolicyOrLegalNotice", policies);
		info.add("PointersToOtherLoTE", selfPointer(list, url));
		JsonArray distributionPoints = new JsonArray();
		distributionPoints.add(url);
		info.add("DistributionPoints", distributionPoints);

		JsonObject lote = new JsonObject();
		lote.add("ListAndSchemeInformation", info);
		if (!empty && !list.services.isEmpty()) {
			JsonArray entities = new JsonArray();
			entities.add(trustedEntity(list));
			lote.add("TrustedEntitiesList", entities);
		}
		JsonObject root = new JsonObject();
		root.add("LoTE", lote);
		return root;
	}

	private static JsonObject trustedEntity(LoteList list) {
		JsonObject information = new JsonObject();
		information.add("TEName", multilingual(TRUSTED_ENTITY_NAME));
		information.add("TETradeName", multilingual(TestKeysAndCerts.ORGANIZATION_IDENTIFIER));
		JsonObject address = new JsonObject();
		address.add("TEElectronicAddress", multilingualUri("mailto:certification@oidf.org"));
		information.add("TEAddress", address);
		information.add("TEInformationURI", multilingualUri(TestKeysAndCerts.getHttpsBaseUrl() + "/trust-anchors"));

		JsonArray services = new JsonArray();
		for (LoteList.Service service : list.services) {
			JsonObject serviceInformation = new JsonObject();
			serviceInformation.addProperty("ServiceTypeIdentifier", service.serviceType());
			serviceInformation.add("ServiceName", multilingual(service.name()));
			JsonObject identity = new JsonObject();
			JsonArray certificates = new JsonArray();
			JsonObject certificate = new JsonObject();
			certificate.addProperty("val", base64Der(service.ca()));
			certificates.add(certificate);
			identity.add("X509Certificates", certificates);
			serviceInformation.add("ServiceDigitalIdentity", identity);
			serviceInformation.add("SchemeServiceDefinitionURI", multilingualUri(service.ca().getTrustAnchorUrl()));
			JsonObject wrapper = new JsonObject();
			wrapper.add("ServiceInformation", serviceInformation);
			services.add(wrapper);
		}

		JsonObject entity = new JsonObject();
		entity.add("TrustedEntityInformation", information);
		entity.add("TrustedEntityServices", services);
		return entity;
	}

	/** Clause 6.3.13 and the profiles: the list points at itself, with the sealing certificate as the pointed-to list's identity. */
	private static JsonArray selfPointer(LoteList list, String url) {
		JsonObject identity = new JsonObject();
		JsonArray certificates = new JsonArray();
		JsonObject certificate = new JsonObject();
		certificate.addProperty("val", base64DerOf(TestKeysAndCerts.getLoteSignerCert()));
		certificates.add(certificate);
		identity.add("X509Certificates", certificates);
		JsonArray identities = new JsonArray();
		identities.add(identity);

		JsonObject qualifiers = new JsonObject();
		qualifiers.addProperty("LoTEType", list.loteType);
		qualifiers.add("SchemeOperatorName", multilingual(SCHEME_OPERATOR_NAME));
		qualifiers.addProperty("MimeType", "application/jwt");
		qualifiers.add("SchemeTypeCommunityRules", multilingualUri(list.schemeTypeCommunityRules));
		qualifiers.addProperty("SchemeTerritory", SCHEME_TERRITORY);
		JsonArray qualifierList = new JsonArray();
		qualifierList.add(qualifiers);

		JsonObject pointer = new JsonObject();
		pointer.addProperty("LoTELocation", url);
		pointer.add("ServiceDigitalIdentities", identities);
		pointer.add("LoTEQualifiers", qualifierList);
		JsonArray pointers = new JsonArray();
		pointers.add(pointer);
		return pointers;
	}

	/** The {@code kid} the reference lists use: the base64 DER of an IssuerSerial naming the sealing certificate. */
	private static String issuerSerialKeyId() {
		try {
			X509CertificateHolder holder = new X509CertificateHolder(TestKeysAndCerts.getLoteSignerCert().getEncoded());
			IssuerSerial issuerSerial = new IssuerSerial(
				new org.bouncycastle.asn1.x509.GeneralNames(new GeneralName(holder.getIssuer())), holder.getSerialNumber());
			return Base64.encode(issuerSerial.getEncoded()).toString();
		} catch (IOException | CertificateEncodingException e) {
			throw new IllegalStateException("Failed to encode the LoTE signer's issuer and serial", e);
		}
	}

	private static String base64Der(TestKeysAndCerts.SuiteCa ca) {
		var encoded = ca.getCert().getEncoded();
		return java.util.Base64.getEncoder().encodeToString(encoded.toByteArray(0, encoded.getSize()));
	}

	private static String base64DerOf(java.security.cert.X509Certificate cert) {
		try {
			return java.util.Base64.getEncoder().encodeToString(cert.getEncoded());
		} catch (CertificateEncodingException e) {
			throw new IllegalStateException(e);
		}
	}

	private static JsonArray multilingual(String value) {
		JsonObject entry = new JsonObject();
		entry.addProperty("lang", "en");
		entry.addProperty("value", value);
		JsonArray array = new JsonArray();
		array.add(entry);
		return array;
	}

	private static JsonArray multilingualUri(String uri) {
		JsonObject entry = new JsonObject();
		entry.addProperty("lang", "en");
		entry.addProperty("uriValue", uri);
		JsonArray array = new JsonArray();
		array.add(entry);
		return array;
	}
}
