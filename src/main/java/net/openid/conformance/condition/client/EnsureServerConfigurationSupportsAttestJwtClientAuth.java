package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class EnsureServerConfigurationSupportsAttestJwtClientAuth extends AbstractValidateJsonArray {

	private static final String environmentVariable = "token_endpoint_auth_methods_supported";

	private static final String[] SET_VALUES = { "attest_jwt_client_auth" };

	private static final String errorMessageNotEnough = "server discovery document contains token_endpoint_auth_methods_supported which does not contain attest_jwt_client_auth";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, environmentVariable, Arrays.asList(SET_VALUES), 1,
			errorMessageNotEnough);
	}

}
