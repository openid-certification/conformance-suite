package net.openid.conformance.vciid2wallet.condition;

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
	@PreEnvironment(strings = "fapi_interaction_id")
	@PostEnvironment(required = {"credential_endpoint_response", "credential_endpoint_response_headers"})
	public Environment evaluate(Environment env) {

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
		headers.addProperty("content-type", "application/json; charset=UTF-8");

		JsonObject response = new JsonObject();

		JsonObject requestBodyJson = env.getElementFromObject("incoming_request", "body_json").getAsJsonObject();
		String credentialConfigId = requestBodyJson.get("credential_configuration_id") != null ? OIDFJSON.getString(requestBodyJson.get("credential_configuration_id")) : null;
		String credentialId = requestBodyJson.get("credential_identifier") != null ? OIDFJSON.getString(requestBodyJson.get("credential_identifier")) : null;

		JsonObject proof = requestBodyJson.getAsJsonObject("proof");
		JsonObject proofs = requestBodyJson.getAsJsonObject("proofs");

		// TODO generate actual credential

		// see: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#section-8.3
		JsonArray credentials = new JsonArray();
		JsonObject credential = new JsonObject();
		// using mock from the spec here for now
		credential.addProperty("credential", env.getString("credential"));
		credentials.add(credential);
		response.add("credentials", credentials);

		// TODO handle notification_id
		// TODO handle transaction_id

		logSuccess("Created credential response object", args("credential_endpoint_response", response, "credential_endpoint_response_headers", headers));

		env.putObject("credential_endpoint_response", response);
		env.putObject("credential_endpoint_response_headers", headers);


		return env;
	}
}
