package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public abstract class AbstractCheckErrorFromTokenEndpointResponseError extends AbstractCondition {

	protected abstract String[] getExpectedError();

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		if (!env.containsObject("token_endpoint_response")) {
			throw error("Couldn't find token endpoint response");
		}

		String error = env.getString("token_endpoint_response", "error");
		if (Strings.isNullOrEmpty(error)) {
			throw error("Couldn't find error field");
		}

		String[] expected = getExpectedError();
		if (!Arrays.asList(expected).contains(error)) {
			throw error("'error' field has unexpected value", args("expected", expected, "actual", error));
		}

		logSuccess("Token Endpoint response error returned expected 'error' of '" + error + "'", args("expected", expected));
		return env;
	}
}
