package io.fintechlabs.testframework.condition.rs;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureBearerAccessTokenNotInParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		String incoming = env.getString("incoming_request", "params.access_token");

		if (!Strings.isNullOrEmpty(incoming)) {
			throw error("Client incorrectly supplied access token in query parameters or form body", args("access_token", incoming));
		} else {
			logSuccess("Client correctly did not send access token in query parameters or form body");
			return env;
		}
	}

}
