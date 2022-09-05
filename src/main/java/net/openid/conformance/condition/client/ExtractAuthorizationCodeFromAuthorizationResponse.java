package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAuthorizationCodeFromAuthorizationResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		String code = env.getString("authorization_endpoint_response", "code");
		if (Strings.isNullOrEmpty(code)) {
			throw error("Couldn't find authorization code in authorization_endpoint_response, 'code' parameter is missing/empty");
		} else {
			env.putString("code", code);
			logSuccess("Found authorization code",
				args("code", code));
			return env;
		}

	}

}
