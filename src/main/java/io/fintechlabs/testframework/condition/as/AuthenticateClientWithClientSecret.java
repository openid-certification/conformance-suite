package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class AuthenticateClientWithClientSecret extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client_authentication")
	@PostEnvironment(strings = "client_authentication_success")
	public Environment evaluate(Environment env) {
		if (env.containsObject("client_authentication_success")) {
			throw error("Found existing client authentication");
		}

		if (!env.containsObject("client_authentication")) {
			throw error("Couldn't find client authentication");
		}

		if (Strings.isNullOrEmpty(env.getString("client_authentication", "method"))) {
			throw error("Couldn't determine client authentication method");
		}

		if (env.getString("client_authentication", "method").equals("client_secret_post")
			|| env.getString("client_authentication", "method").equals("client_secret_basic")) {

			String expected = env.getString("client", "client_secret");
			String actual = env.getString("client_authentication", "client_secret");

			if (!Strings.isNullOrEmpty(expected)
				&& expected.equals(actual)) {

				logSuccess("Authenticated the client", args("client_authentication_success", env.getString("client_authentication", "method")));

				env.putString("client_authentication_success", env.getString("client_authentication", "method"));

				return env;

			} else {
				throw error("Mismatch client secrets", args("expected", expected, "actual", actual));
			}

		} else {
			throw error("Can't handle client method " + env.getString("client_authentication", "method"));
		}

	}

}
