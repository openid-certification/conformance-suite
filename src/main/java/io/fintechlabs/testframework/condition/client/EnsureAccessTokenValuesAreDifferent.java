package io.fintechlabs.testframework.condition.client;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureAccessTokenValuesAreDifferent extends AbstractCondition {
	@Override
	@PreEnvironment(strings = {"first_access_token", "second_access_token"})
	public Environment evaluate(Environment env)
	{
		String firstAccessToken = env.getString("first_access_token");
		String secondAccessToken = env.getString("second_access_token");

		if(firstAccessToken==null) {
			throw error("first_access_token is null");
		}
		if(secondAccessToken==null) {
			throw error("second_access_token is null");
		}

		if(firstAccessToken.equals(secondAccessToken)) {
			throw error("Access token values are not different");
		}

		logSuccess("Access token values are not the same",
			args("first_access_token", firstAccessToken, "second_access_token", secondAccessToken));
		return env;
	}
}
