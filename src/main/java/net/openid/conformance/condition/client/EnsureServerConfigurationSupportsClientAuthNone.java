package net.openid.conformance.condition.client;

import com.google.common.collect.ImmutableList;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

public class EnsureServerConfigurationSupportsClientAuthNone extends AbstractValidateJsonArray{
	private static final String ENVIRONMENT_VARIABLE = "token_endpoint_auth_methods_supported";

	private static final List<String> EXPECTED_VALUES = ImmutableList.of("none");

	private static final String ERROR_MESSAGE_NOT_ENOUGH = "server discovery document does not contain none in token_endpoint_auth_methods_supported";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		return validate(env, ENVIRONMENT_VARIABLE, EXPECTED_VALUES, 1,
			ERROR_MESSAGE_NOT_ENOUGH);
	}
}
