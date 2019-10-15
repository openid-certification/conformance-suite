package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureInvalidRequestErrorBackchannelAuthenticationEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("backchannel_authentication_endpoint_response", "error");
		String expected = "invalid_request";

		if (Strings.isNullOrEmpty(error)) {
			throw error("Expected 'error' field not found");
		} else if (!error.equals(expected)) {
			throw error("'error' field has unexpected value", args("expected", expected, "actual", error));
		} else {
			logSuccess("Back channel authentication endpoint returned expected 'error' of '" + expected + "'", args("error", error));
			return env;
		}
	}
}
