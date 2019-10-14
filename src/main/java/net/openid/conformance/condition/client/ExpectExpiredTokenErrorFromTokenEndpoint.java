package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExpectExpiredTokenErrorFromTokenEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment( required = "token_endpoint_response" )
	public Environment evaluate(Environment env) {
		String error = env.getString("token_endpoint_response", "error");

		if (Strings.isNullOrEmpty(error)) {
			throw error("error missing from token_endpoint_response.");
		}

		String expected = "expired_token";

		if (!error.equals(expected)) {
			throw error("error value is incorrect",
				args("expected", expected, "actual", error));
		}

		logSuccess("error parameter is correctly '"+error+"'");

		return env;
	}
}
