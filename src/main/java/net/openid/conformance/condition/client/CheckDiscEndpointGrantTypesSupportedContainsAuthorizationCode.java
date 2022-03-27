package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Arrays;

public class CheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode extends AbstractValidateJsonArray {

	private static final String discoveryKey = "grant_types_supported";

	private static final String EXPECTED_VALUE = "authorization_code";
	private static final int minimumMatchesRequired = 1;

	private static final String errorMessageNotEnough = "The server does not support the required grant types.";

	@Override
	@PreEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement supportedAuthMethods = env.getElementFromObject("server", discoveryKey);

		if (supportedAuthMethods == null) {
			logSuccess("server discovery document does not contain "+discoveryKey+", so by default authorization_code is supported");
			return env;
		}
		return validate(env, discoveryKey, Arrays.asList(EXPECTED_VALUE), minimumMatchesRequired, errorMessageNotEnough);

	}
}
