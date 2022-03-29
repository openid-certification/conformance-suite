package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class IdmvpCheckClaimsSupported extends AbstractValidateJsonArray {

	private static final String environmentVariable = "claims_supported";

	private static final String[] EXPECTED_VALUES = {
		"name",
		"given_name",
		"family_name",
		"email",
		"birthdate",
		"phone_number",
		"address"
	};

	private static final int minimumMatchesRequired = EXPECTED_VALUES.length;

	private static final String errorMessageNotEnough = "The server does not support the required claims.";
	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, environmentVariable, Arrays.asList(EXPECTED_VALUES), minimumMatchesRequired, errorMessageNotEnough);
	}

}
