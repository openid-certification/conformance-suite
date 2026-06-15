package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequestOrInvalidAuthorizationDetails extends AbstractCondition {

	private static final List<String> EXPECTED_VALUES = ImmutableList.of("invalid_request", "invalid_authorization_details");

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

		if (!EXPECTED_VALUES.contains(error)) {
			throw error("'error' field has unexpected value", args("expected", EXPECTED_VALUES, "actual", error));
		}

		logSuccess("Backchannel Authentication Endpoint response returned an expected 'error'",
			args("expected", EXPECTED_VALUES, "actual", error));
		return env;
	}
}
