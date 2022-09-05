package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateRedirectUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "token_endpoint_request" })
	public Environment evaluate(Environment env) {

		String expected = env.getString("client", "redirect_uri");
		String actual = env.getString("token_endpoint_request", "body_form_params.redirect_uri");

		if (Strings.isNullOrEmpty(expected)) {
			throw error("Couldn't find redirect uri to compare");
		}

		if (expected.equals(actual)) {
			logSuccess("Found redirect uri", args("redirect_uri", actual));
			return env;
		} else {
			throw error("Didn't find matching redirect uri", args("expected", expected, "actual", actual));
		}

	}

}
