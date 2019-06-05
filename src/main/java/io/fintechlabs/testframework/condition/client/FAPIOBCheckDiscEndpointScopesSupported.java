package io.fintechlabs.testframework.condition.client;

import java.util.Arrays;

import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class FAPIOBCheckDiscEndpointScopesSupported extends ValidateJsonArray {

	private static final String environmentVariable = "scopes_supported";

	private static final String[] SET_VALUES = new String[] { "openid", "accounts", "payments" };
	private static final int minimumMatchesRequired = 3;

	private static final String errorMessageNotEnough = "The server does not support enough of the required scopes";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired,
				errorMessageNotEnough);

	}

}
