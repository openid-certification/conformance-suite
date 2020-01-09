package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class CheckErrorFromAuthorizationEndpointIsOneThatRequiredAUserInterface extends AbstractCondition {

	// as per https://openid.net/specs/openid-connect-core-1_0.html#AuthError
	// matches https://github.com/rohe/oidctest/blob/master/test_tool/cp/test_op/flows/OP-prompt-none-NotLoggedIn.json#L38
	private static final List<String> EXPECTED_VALUES = ImmutableList.of(
		"interaction_required",
		"login_required",
		"account_selection_required",
		"consent_required"
	);

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {

		String error = env.getString("authorization_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("Expected 'error' field not found");
		} else if (!EXPECTED_VALUES.contains(error)) {
			throw error("'error' field has an unexpected value", args("permitted", EXPECTED_VALUES, "actual", error));
		} else {
			logSuccess("Authorization endpoint returned one of the permitted errors", args("permitted", EXPECTED_VALUES, "actual", error));
			return env;
		}
	}
}
