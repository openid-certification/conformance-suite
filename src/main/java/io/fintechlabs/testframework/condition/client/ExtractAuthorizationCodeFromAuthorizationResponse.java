package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractAuthorizationCodeFromAuthorizationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		String code = env.getString("authorization_endpoint_response", "code");
		if (Strings.isNullOrEmpty(code)) {
			throw error("Couldn't find authorization code in authorization_endpoint_response");
		} else {
			env.putString("code", code);
			logSuccess("Found authorization code",
				args("code", code));
			return env;
		}

	}

}
