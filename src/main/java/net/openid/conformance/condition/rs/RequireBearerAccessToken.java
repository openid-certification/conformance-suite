package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RequireBearerAccessToken extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "incoming_access_token" })
	public Environment evaluate(Environment env) {

		String actual = env.getString("incoming_access_token");
		String expected = env.getString("access_token");

		if (expected == null) {
			throw error("This endpoint must be called with an access token, but a suitable access token has not been created in this run of the test.");
		}

		if (!Strings.isNullOrEmpty(actual) && actual.equals(expected)) {
			logSuccess("Found access token in request", args("actual", Strings.nullToEmpty(actual)));
			return env;
		} else {
			throw error("Invalid access token ", args("expected", Strings.nullToEmpty(expected), "actual", Strings.nullToEmpty(actual)));
		}

	}

}
