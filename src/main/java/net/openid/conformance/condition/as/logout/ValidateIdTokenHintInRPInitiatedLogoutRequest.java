package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateIdTokenHintInRPInitiatedLogoutRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"end_session_endpoint_http_request_params", "all_issued_id_tokens"})
	public Environment evaluate(Environment env) {
		String idTokenHint = env.getString("end_session_endpoint_http_request_params", "id_token_hint");
		JsonObject issuedIdTokens = env.getObject("all_issued_id_tokens");
		if(!issuedIdTokens.has(idTokenHint)) {
			throw error("Invalid id_token_hint, not an id_token issued by this test instance.",
						args("id_token_hint", idTokenHint, "issued_id_tokens", issuedIdTokens.keySet()));
		}
		logSuccess("id_token_hint was issued by this test instance", args("id_token_hint", idTokenHint));
		return env;
	}

}
