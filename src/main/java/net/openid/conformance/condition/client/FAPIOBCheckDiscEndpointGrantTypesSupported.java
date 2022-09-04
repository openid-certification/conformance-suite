package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class FAPIOBCheckDiscEndpointGrantTypesSupported extends AbstractValidateJsonArray {

	private static final String environmentVariable = "grant_types_supported";

	private static final String[] SET_VALUES = { "authorization_code", "client_credentials" };
	private static final int minimumMatchesRequired = 2;

	private static final String errorMessageNotEnough = "The server does not support enough of the required grant types.";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), minimumMatchesRequired,
				errorMessageNotEnough);

	}

}
