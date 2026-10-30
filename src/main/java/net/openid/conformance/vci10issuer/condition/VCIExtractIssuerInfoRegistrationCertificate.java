package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;

/**
 * Extracts the registration certificate (WRPRC, ETSI TS 119 475) from the {@code registration_cert}
 * element of {@code issuer_info}, which is optional, so this stores nothing when there is none. ETSI
 * TS 119 472-3 says only that the element's data is the certificate; its OpenID4VP counterpart, ETSI
 * TS 119 472-2, has the base64url encoding of the serialized certificate, while issuers send the
 * compact JWS itself, so both are accepted and the encoding found is recorded.
 */
public class VCIExtractIssuerInfoRegistrationCertificate extends AbstractCondition {

	public static final String ENV_KEY = "issuer_info_registration_certificate";

	@Override
	@PreEnvironment(required = VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonElement issuerInfoEl = env.getElementFromObject(VCIExtractIssuerInfoFromSignedCredentialIssuerMetadata.ENV_KEY, "issuer_info");
		if (issuerInfoEl == null || !issuerInfoEl.isJsonArray()) {
			throw error("'issuer_info' is not an array", args("issuer_info", issuerInfoEl));
		}
		JsonArray certificates = VCIEnsureIssuerInfoContainsOneRegistrarDataset.elementsWithFormat(issuerInfoEl.getAsJsonArray(), "registration_cert");
		if (certificates.isEmpty()) {
			log("'issuer_info' has no 'registration_cert' element; a registration certificate is optional",
				args("issuer_info", issuerInfoEl));
			return env;
		}

		JsonElement data = certificates.get(0).getAsJsonObject().get("data");
		if (data == null || !data.isJsonPrimitive() || !data.getAsJsonPrimitive().isString()) {
			throw error("The 'data' of the 'registration_cert' element of 'issuer_info' is not a string",
				args("registration_cert", certificates.get(0)));
		}
		String value = OIDFJSON.getString(data);

		String encoding = "compact JWS";
		SignedJWT jwt = parseOrNull(value);
		if (jwt == null) {
			encoding = "base64url encoding of the compact JWS";
			try {
				jwt = parseOrNull(new String(Base64.getUrlDecoder().decode(value), StandardCharsets.US_ASCII));
			} catch (IllegalArgumentException e) {
				jwt = null;
			}
		}
		if (jwt == null) {
			throw error("The 'data' of the 'registration_cert' element of 'issuer_info' is neither a signed JWT nor the base64url"
					+ " encoding of one", args("data", value));
		}

		JsonObject certificate = new JsonObject();
		certificate.addProperty("value", jwt.serialize());
		certificate.addProperty("encoding", encoding);
		certificate.add("header", JsonParser.parseString(jwt.getHeader().toString()));
		certificate.add("payload", JsonParser.parseString(jwt.getPayload().toString()));
		env.putObject(ENV_KEY, certificate);

		logSuccess("Extracted the registration certificate from 'issuer_info'; its 'data' is the " + encoding,
			args("header", certificate.get("header"), "payload", certificate.get("payload"), "encoding", encoding));
		return env;
	}

	private static SignedJWT parseOrNull(String value) {
		try {
			return SignedJWT.parse(value);
		} catch (ParseException e) {
			return null;
		}
	}
}
