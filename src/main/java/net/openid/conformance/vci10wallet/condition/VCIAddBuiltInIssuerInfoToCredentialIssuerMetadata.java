package net.openid.conformance.vci10wallet.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.SuiteWrprc;
import net.openid.conformance.util.TestKeysAndCerts;
import net.openid.conformance.util.X509CertificateUtil;
import org.bouncycastle.asn1.x500.style.BCStyle;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds the suite's {@code issuer_info} (ETSI TS 119 472-3 clause 4.2.3, {@link SuiteWrprc#issuerInfo})
 * to the credential issuer metadata when the metadata is signed with a certificate of the OpenID
 * Foundation, such as the built-in access certificate {@link VCIGenerateSignedCredentialIssuerMetadata}
 * uses when no 'Signing JWK' is configured: the suite's registration certificate is bound to the access
 * certificate by the organizationIdentifier, so it fits no other. The registered attestations are the
 * credential configurations of the metadata: an SD-JWT VC one by its {@code vct}, an mdoc one by its
 * {@code doctype}, as TS 119 475 Credential objects with the {@code meta} of an OpenID4VP DCQL query.
 */
public class VCIAddBuiltInIssuerInfoToCredentialIssuerMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "credential_issuer_metadata"})
	public Environment evaluate(Environment env) {
		JsonObject jwks;
		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (credentialSigningJwkEl == null || !credentialSigningJwkEl.isJsonObject()) {
			jwks = TestKeysAndCerts.getWrpac().jwksJson();
		} else {
			jwks = JWKUtil.createJwksObjectFromJwkObjects(credentialSigningJwkEl.getAsJsonObject());
		}
		String organizationIdentifier = signingCertificateOrganizationIdentifier(jwks);
		if (!TestKeysAndCerts.ORGANIZATION_IDENTIFIER.equals(organizationIdentifier)) {
			log("The issuer metadata is not signed with a certificate of the OpenID Foundation, which the suite's registration"
					+ " certificate is bound to; the suite sends no 'issuer_info'",
				args("signing_certificate_organization_identifier", organizationIdentifier));
			return env;
		}

		JsonObject metadata = env.getObject("credential_issuer_metadata");
		JsonElement configsEl = metadata.get("credential_configurations_supported");
		if (configsEl == null || !configsEl.isJsonObject()) {
			throw error("The suite's credential issuer metadata has no 'credential_configurations_supported'", args("metadata", metadata));
		}

		JsonArray providesAttestations = providesAttestations(configsEl.getAsJsonObject());
		JsonArray issuerInfo = SuiteWrprc.issuerInfo(providesAttestations);
		metadata.add("issuer_info", issuerInfo);
		env.putObject("credential_issuer_metadata", metadata);

		logSuccess("Added the suite's 'issuer_info' to the credential issuer metadata, with a registration certificate issued"
				+ " under the WRPRC provider CA served at " + TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER.getTrustAnchorUrl(),
			args("issuer_info", issuerInfo,
				"registration_certificate_payload", registrationCertificatePayload(issuerInfo),
				"wrprc_provider_ca_url", TestKeysAndCerts.SuiteCa.WRPRC_PROVIDER.getTrustAnchorUrl()));
		return env;
	}

	/** One TS 119 475 Credential object per distinct format and type among {@code configurations}. */
	static JsonArray providesAttestations(JsonObject configurations) {
		Map<String, JsonObject> byKey = new LinkedHashMap<>();
		for (String id : configurations.keySet()) {
			JsonElement configEl = configurations.get(id);
			if (!configEl.isJsonObject()) {
				continue;
			}
			JsonObject config = configEl.getAsJsonObject();
			String format = stringOrNull(config.get("format"));
			JsonObject meta = new JsonObject();
			if ("dc+sd-jwt".equals(format) && stringOrNull(config.get("vct")) != null) {
				JsonArray vctValues = new JsonArray();
				vctValues.add(stringOrNull(config.get("vct")));
				meta.add("vct_values", vctValues);
			} else if ("mso_mdoc".equals(format) && stringOrNull(config.get("doctype")) != null) {
				meta.addProperty("doctype_value", stringOrNull(config.get("doctype")));
			} else {
				continue;
			}
			JsonObject credential = new JsonObject();
			credential.addProperty("format", format);
			credential.add("meta", meta);
			byKey.putIfAbsent(format + " " + meta, credential);
		}
		JsonArray providesAttestations = new JsonArray();
		byKey.values().forEach(providesAttestations::add);
		return providesAttestations;
	}

	/** The payload of the registration certificate in {@code issuerInfo}, a compact JWS. */
	private JsonObject registrationCertificatePayload(JsonArray issuerInfo) {
		for (JsonElement element : issuerInfo) {
			JsonObject obj = element.getAsJsonObject();
			if ("registration_cert".equals(stringOrNull(obj.get("format")))) {
				String jws = OIDFJSON.getString(obj.get("data"));
				try {
					return JsonParser.parseString(JWSObject.parse(jws).getPayload().toString()).getAsJsonObject();
				} catch (ParseException e) {
					throw error("The suite's registration certificate is not a JWS", e, args("registration_certificate", jws));
				}
			}
		}
		throw error("The suite's 'issuer_info' has no registration certificate", args("issuer_info", issuerInfo));
	}

	private static String stringOrNull(JsonElement element) {
		return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() ? OIDFJSON.getString(element) : null;
	}

	/** The organizationIdentifier of the certificate of the signing key in {@code jwks}, or null. */
	private static String signingCertificateOrganizationIdentifier(JsonObject jwks) {
		try {
			return X509CertificateUtil.subjectRdnValue(JWKUtil.getSigningKeyCertificateChain(jwks).get(0), BCStyle.ORGANIZATION_IDENTIFIER);
		} catch (JWKUtil.SigningKeyChainException e) {
			// no single signing key with a certificate: the metadata signer reports that
			return null;
		}
	}
}
