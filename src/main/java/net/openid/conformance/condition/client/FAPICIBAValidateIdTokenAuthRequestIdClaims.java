package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPICIBAValidateIdTokenAuthRequestIdClaims extends AbstractCondition {

	@Override
	@PreEnvironment( required = { "token_endpoint_request_form_parameters", "id_token" } )
	public Environment evaluate(Environment env) {
		String authReqIdClaim = env.getString("id_token", "claims.urn:openid:params:jwt:claim:auth_req_id");

		String authReqIdRequest = env.getString("token_endpoint_request_form_parameters", "auth_req_id");

		if (!authReqIdClaim.equals(authReqIdRequest)) {
			throw error("urn:openid:params:jwt:claim:auth_req_id claim in the ID Token did not match the auth_req_id in the request", args("expected", authReqIdRequest, "actual", authReqIdClaim));
		}

		logSuccess("urn:openid:params:jwt:claim:auth_req_id claim in the ID Token matched the auth_req_id in the request.");

		return env;
	}
}
