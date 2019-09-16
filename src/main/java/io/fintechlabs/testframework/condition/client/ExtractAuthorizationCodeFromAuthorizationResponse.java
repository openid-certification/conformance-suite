package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractAuthorizationCodeFromAuthorizationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "callback_params")
	public Environment evaluate(Environment env) {
		String code = env.getString("callback_params", "code");
		if (Strings.isNullOrEmpty(code)) {
			throw error("Couldn't find authorization code in callback");
		} else {
			env.putString("code", code);
			logSuccess("Found authorization code",
				args("code", code));
			return env;
		}

	}

}
