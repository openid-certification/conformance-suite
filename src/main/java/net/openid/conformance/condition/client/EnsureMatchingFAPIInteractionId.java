package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMatchingFAPIInteractionId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_endpoint_response_headers", strings = "fapi_interaction_id")
	public Environment evaluate(Environment env) {

		// get the client ID from the configuration
		String expected = env.getString("fapi_interaction_id");
		String actual = env.getString("resource_endpoint_response_headers", "x-fapi-interaction-id");

		if (!Strings.isNullOrEmpty(expected) && expected.equalsIgnoreCase(actual)) {
			logSuccess("Interaction ID matches, ignoring case", args("fapi_interaction_id", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Mismatch between interaction IDs", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
