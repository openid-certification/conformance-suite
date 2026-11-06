package net.openid.conformance.poppy.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsurePoppyTokenTypeIsBearer extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement tokenType = env.getElementFromObject("token_endpoint_response", "token_type");
		if (tokenType == null || !tokenType.isJsonPrimitive() || !tokenType.getAsJsonPrimitive().isString()) {
			throw error("The token response has no 'token_type' string, which RFC 6749 section 5.1 requires",
				args("token_endpoint_response", env.getObject("token_endpoint_response")));
		}
		String value = OIDFJSON.getString(tokenType);
		if (!"bearer".equalsIgnoreCase(value)) {
			throw error("The token response has token_type '" + value + "'. This module did not send a DPoP proof, so a DPoP-bound token cannot be used; the Company must issue a Bearer token when no DPoP proof is presented.",
				args("token_type", value));
		}
		logSuccess("The access token is a Bearer token", args("token_type", value));
		return env;
	}
}
