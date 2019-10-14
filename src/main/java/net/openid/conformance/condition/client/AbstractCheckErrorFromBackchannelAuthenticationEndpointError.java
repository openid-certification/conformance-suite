package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractCheckErrorFromBackchannelAuthenticationEndpointError extends AbstractCondition {

	protected abstract String getExpectedError();

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		if (!env.containsObject("backchannel_authentication_endpoint_response")) {
			throw error("Couldn't find backchannel authentication endpoint response");
		}

		String error = env.getString("backchannel_authentication_endpoint_response", "error");
		if (Strings.isNullOrEmpty(error)) {
			throw error("Couldn't find error field");
		}

		String expected = getExpectedError();
		if (!expected.equals(error)) {
			throw error("'error' field has unexpected value", args("expected", expected, "actual", error));
		}

		logSuccess("Token Endpoint response error returned expected 'error' of '" + expected + "'", args("error", error));
		return env;
	}
}
