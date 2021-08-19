package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class EnsureServerConfigurationSupportsClientSecretPost extends AbstractValidateJsonArray {

	private static final String ENVIRONMENT_VARIABLE = "token_endpoint_auth_methods_supported";

	private static final String[] SET_VALUES = { "client_secret_post" };

	private static final String ERROR_MESSAGE_NOT_ENOUGH = "server discovery document does not contain client_secret_post in token_endpoint_auth_methods_supported";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, ENVIRONMENT_VARIABLE, Arrays.asList(SET_VALUES), 1,
			ERROR_MESSAGE_NOT_ENOUGH);
	}

}
