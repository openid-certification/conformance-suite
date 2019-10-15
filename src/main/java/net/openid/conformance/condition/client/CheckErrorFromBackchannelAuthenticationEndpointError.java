package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CheckErrorFromBackchannelAuthenticationEndpointError extends AbstractCondition {

	private static final List<String> EXPECTED_VALUES = ImmutableList.of("access_denied", "invalid_request", "invalid_client");

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("backchannel_authentication_endpoint_response", "error");
		if (Strings.isNullOrEmpty(error)) {
			throw error("Couldn't find error field");
		}

		if (!EXPECTED_VALUES.contains(error)) {
			throw error("'error' field has unexpected value", args("expected", EXPECTED_VALUES, "actual", error));
		}

		logSuccess("Backchannel authentication endpoint returned an expected error", args("expected", EXPECTED_VALUES, "actual", error));

		return env;
	}
}
