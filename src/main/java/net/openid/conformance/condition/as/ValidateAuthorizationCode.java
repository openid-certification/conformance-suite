package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateAuthorizationCode extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "authorization_code", required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String expected = env.getString("authorization_code");
		String actual = env.getString("token_endpoint_request", "body_form_params.code");

		if (Strings.isNullOrEmpty(expected)) {
			throw error("Couldn't find authorization code to compare");
		}

		if (expected.equals(actual)) {
			logSuccess("Found authorization code", args("authorization_code", actual));
			return env;
		} else {
			throw error("Didn't find matching authorization code", args("expected", expected, "actual", actual));
		}
	}

}
