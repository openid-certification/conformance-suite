package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class FAPICIBACheckDiscEndpointGrantTypesSupported extends AbstractValidateJsonArray {

	private static final String environmentVariable = "grant_types_supported";

	private static final String EXPECTED_VALUE = "urn:openid:params:grant-type:ciba";

	private static final String errorMessageNotEnough = "The server does not support enough of the required grant types.";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(EXPECTED_VALUE), 1, errorMessageNotEnough);

	}

}
