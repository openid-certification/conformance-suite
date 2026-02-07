package net.openid.conformance.vci10wallet.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCICreateCredentialEndpointResponse extends AbstractCondition {

	@SuppressWarnings("unused")
	@Override
	@PreEnvironment(strings = "fapi_interaction_id", required = "credential_issuance")
	@PostEnvironment(required = {"credential_endpoint_response", "credential_endpoint_response_headers"})
	public Environment evaluate(Environment env) {

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
		headers.addProperty("content-type", "application/json");

		JsonObject response = new JsonObject();

		JsonObject requestBodyJson = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		String credentialConfigId = requestBodyJson.get("credential_configuration_id") != null ? OIDFJSON.getString(requestBodyJson.get("credential_configuration_id")) : null;
		String credentialId = requestBodyJson.get("credential_identifier") != null ? OIDFJSON.getString(requestBodyJson.get("credential_identifier")) : null;

		JsonObject proof = requestBodyJson.getAsJsonObject("proof");
		JsonObject proofs = requestBodyJson.getAsJsonObject("proofs");

		// Get credentials array from credential_issuance (populated by CreateSdJwtCredential or CreateMdocCredentialForVCI)
		// Per VCI spec F.1 and F.3, the issuer SHOULD issue a Credential for each key in attested_keys
		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3
		JsonObject credentialIssuance = env.getObject("credential_issuance");
		JsonArray credentials = credentialIssuance.getAsJsonArray("credentials");

		response.add("credentials", credentials);

		env.putObject("credential_endpoint_response", response);
		env.putObject("credential_endpoint_response_headers", headers);

		logSuccess("Created credential response object",
			args("credential_endpoint_response", response,
				"credential_endpoint_response_headers", headers,
				"credential_count", credentials.size()));

		return env;
	}
}
