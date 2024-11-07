package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractCheckErrorFromTokenEndpointResponseError extends AbstractCheckErrorFromResponseError {

	@Override
	protected String getResponseKey() {
		return "token_endpoint_response";
	}

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		return super.evaluate(env);
	}
}
