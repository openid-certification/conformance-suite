package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFValidateSecurityEventTokenAudClaim extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement setAudienceElement = env.getElementFromObject("set_token", "claims.aud");
		if (setAudienceElement == null) {
			throw error("Could not find required 'aud' claim in verification token");
		}

		if (!(setAudienceElement.isJsonPrimitive() || setAudienceElement.isJsonArray())) {
			throw error("SET claim 'aud' must be a string or an array of strings");
		}

		logSuccess("SET claim 'aud' claim is a string or an array of strings", args("aud", setAudienceElement));

		return env;
	}
}
