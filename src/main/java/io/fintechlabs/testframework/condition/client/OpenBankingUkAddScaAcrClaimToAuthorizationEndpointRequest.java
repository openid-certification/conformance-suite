package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class OpenBankingUkAddScaAcrClaimToAuthorizationEndpointRequest extends AbstractAddClaimToAuthorizationEndpointRequest {

	@Override
	@PreEnvironment(required = "authorization_endpoint_request")
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		return addClaim(env, "acr", "urn:openbanking:psd2:sca", true);
	}

}
