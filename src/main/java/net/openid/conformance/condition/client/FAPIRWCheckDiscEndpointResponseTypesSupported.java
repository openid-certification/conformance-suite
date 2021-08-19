package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class FAPIRWCheckDiscEndpointResponseTypesSupported extends AbstractValidateResponseTypesArray {

	private static final String environmentVariable = "response_types_supported";

	private static final String[] SET_VALUES = {"code id_token"};

	private static final String errorMessageNotEnough = "No matching value from server";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1,
			errorMessageNotEnough);

	}
}
