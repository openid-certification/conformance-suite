package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpHeaders;

public class GenerateCredentialNonceResponse extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "credential_issuer_nonce"})
	public Environment evaluate(Environment env) {

		String fapiInteractionId = env.getString("fapi_interaction_id");
		if (Strings.isNullOrEmpty(fapiInteractionId)) {
			throw error("Couldn't find FAPI Interaction ID");
		}

		JsonObject headers = new JsonObject();
		headers.addProperty("x-fapi-interaction-id", fapiInteractionId);
		headers.addProperty("content-type", "application/json; charset=UTF-8");
		headers.addProperty(HttpHeaders.CACHE_CONTROL, "no-store");

		String nonce = env.getString("credential_issuer_nonce");

		JsonObject response = new JsonObject();
		response.addProperty("c_nonce", nonce);

		env.putObject("credential_nonce_response", response);
		env.putObject("credential_nonce_response_headers", headers);

		logSuccess("Created credential nonce response", args("credential_issuer_nonce_response", response, "credential_nonce_response_headers", headers));

		return env;
	}
}
