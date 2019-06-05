package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddAccountRequestIdToAuthorizationEndpointRequest extends AbstractAddClaimToAuthorizationEndpointRequest {

	@Override
	@PreEnvironment(strings = "account_request_id", required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		return addClaim(env, "openbanking_intent_id", env.getString("account_request_id"), true);
	}

}
