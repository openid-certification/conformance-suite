package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

public abstract class AbstractCheckErrorFromBackchannelAuthenticationEndpointError extends AbstractCondition {

	protected abstract Set<String> getExpectedErrors();

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {
		String error = env.getString("backchannel_authentication_endpoint_response", "error");
		if (Strings.isNullOrEmpty(error)) {
			throw error("Couldn't find error field");
		}

		Set<String> expected = getExpectedErrors();
		if (!expected.contains(error)) {
			throw error("'error' field has unexpected value", args("expected", expected, "actual", error));
		}

		logSuccess("Backchannel Authentication Endpoint response returned an expected 'error'",
			args("expected", expected, "actual", error));
		return env;
	}
}
