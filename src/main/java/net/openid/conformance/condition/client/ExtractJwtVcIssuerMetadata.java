package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Extracts the JWT VC Issuer Metadata from a 200 response of the metadata endpoint and stores it
 * as {@code jwt_vc_issuer_metadata}, provided its {@code issuer} is identical to the {@code iss}
 * it was fetched for. Metadata whose {@code issuer} differs must not be used, so it is not stored.
 *
 * Any other HTTP status is reported by the status code check; the response is then not used.
 */
public class ExtractJwtVcIssuerMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = { CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY, "status_list_token_jwt_vc_issuer" })
	public Environment evaluate(Environment env) {
		String responseKey = CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY;
		String iss = env.getString("status_list_token_jwt_vc_issuer", "iss");
		String url = env.getString("status_list_token_jwt_vc_issuer", "metadata_url");

		int status = env.getInteger(responseKey, "status");
		if (status != 200) {
			log("The JWT VC Issuer Metadata response is not used because it was not a 200 response",
				args("url", url, "http_status", status));
			return env;
		}

		JsonElement body = env.getElementFromObject(responseKey, "body_json");
		if (body == null || !body.isJsonObject()) {
			throw error("The JWT VC Issuer Metadata response body is not a JSON object",
				args("url", url, "body", env.getElementFromObject(responseKey, "body")));
		}
		JsonObject metadata = body.getAsJsonObject();

		JsonElement issuerEl = metadata.get("issuer");
		if (issuerEl == null || !issuerEl.isJsonPrimitive() || !issuerEl.getAsJsonPrimitive().isString()) {
			throw error("The JWT VC Issuer Metadata does not contain an 'issuer' string",
				args("url", url, "metadata", metadata));
		}
		String issuer = OIDFJSON.getString(issuerEl);
		if (!issuer.equals(iss)) {
			throw error("The 'issuer' in the JWT VC Issuer Metadata is not identical to the iss claim it was fetched for, so the metadata must not be used",
				args("url", url, "issuer", issuer, "iss", iss,
					"iss_source", env.getString("status_list_token_jwt_vc_issuer", "iss_source")));
		}

		env.putObject("jwt_vc_issuer_metadata", metadata);
		logSuccess("Extracted the JWT VC Issuer Metadata, its issuer matches the iss claim",
			args("url", url, "metadata", metadata));
		return env;
	}
}
