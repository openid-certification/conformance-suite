package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckTokenTypeIsDpop extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {

		String tokenType = env.getString("token_endpoint_response", "token_type");
		if (Strings.isNullOrEmpty(tokenType)) {
			throw error("Couldn't find token type");
		}

		if (!"dpop".equalsIgnoreCase(tokenType)) {
			throw error("Token type is not DPoP");
		}

		logSuccess("Token type is DPoP");

		return env;
	}
}
