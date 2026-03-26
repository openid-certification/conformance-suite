package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public abstract class AbstractCheckDiscEndpointTokenEndpointAuthMethodsSupported extends AbstractValidateJsonArray {

	private static final String ENVIRONMENT_VARIABLE = "token_endpoint_auth_methods_supported";

	private static final String ERROR_MESSAGE = "No matching value from server";

	protected abstract List<String> getAcceptedAuthMethods();

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {
		return validate(env, ENVIRONMENT_VARIABLE, getAcceptedAuthMethods(), 1, ERROR_MESSAGE);
	}

}
