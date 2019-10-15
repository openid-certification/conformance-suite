package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureAccessTokenValuesAreDifferent extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"first_access_token", "second_access_token"})
	public Environment evaluate(Environment env)
	{
		String firstAccessToken = env.getString("first_access_token", "value");
		String secondAccessToken = env.getString("second_access_token", "value");

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
