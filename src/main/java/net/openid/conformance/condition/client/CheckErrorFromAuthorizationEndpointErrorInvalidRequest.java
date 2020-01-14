package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CheckErrorFromAuthorizationEndpointErrorInvalidRequest extends AbstractCondition {

	private static final List<String> EXPECTED_VALUES = ImmutableList.of("invalid_request");

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("authorization_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("Expected 'error' field not found");
		} else if (!EXPECTED_VALUES.contains(error)) {
			throw error("'error' field has unexpected value", args("expected", EXPECTED_VALUES, "actual", error));
		} else {
			logSuccess("Authorization endpoint returned expected error", args("expected", EXPECTED_VALUES, "actual", error));
			return env;
		}
	}
}
