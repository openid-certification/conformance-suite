package net.openid.conformance.condition.as;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureMatchingRedirectUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "client", "authorization_endpoint_request" })
	public Environment evaluate(Environment env) {
		// get the client ID from the configuration
		String expected = env.getString("client", "redirect_uri");
		String actual = env.getString("authorization_endpoint_request", "params.redirect_uri");

		if (!Strings.isNullOrEmpty(expected) && expected.equals(actual)) {
			logSuccess("Redirect URI matched",
				args("actual", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Mismatch between redirect URI", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
