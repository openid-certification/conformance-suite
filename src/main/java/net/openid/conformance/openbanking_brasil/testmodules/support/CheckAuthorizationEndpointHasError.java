package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckAuthorizationEndpointHasError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		String error = env.getString("authorization_endpoint_response", "error");
		if (Strings.isNullOrEmpty(error)) {
			throw error("The authorization was expected to fail, but the server returned a valid response from the authorization endpoint", env.getObject("authorization_endpoint_response"));
		}

		logSuccess("Error from authorization endpoint, as expected", args("error", error));
		return env;
	}
}
