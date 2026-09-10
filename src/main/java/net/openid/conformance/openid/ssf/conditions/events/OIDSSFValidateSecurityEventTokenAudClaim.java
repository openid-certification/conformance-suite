package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFValidateSecurityEventTokenAudClaim extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement setAudienceElement = env.getElementFromObject("set_token", "claims.aud");
		if (setAudienceElement == null) {
			throw error("Could not find required 'aud' claim in verification token");
		}

		if (setAudienceElement.isJsonArray()) {
			if (setAudienceElement.getAsJsonArray().isEmpty()) {
				throw error("SET claim 'aud' is an empty array; it must name at least one audience", args("aud", setAudienceElement));
			}
			for (JsonElement member : setAudienceElement.getAsJsonArray()) {
				if (!isNonEmptyString(member)) {
					throw error("SET claim 'aud' must be a string or an array of non-empty strings", args("aud", setAudienceElement, "member", member));
				}
			}
		} else if (!isNonEmptyString(setAudienceElement)) {
			throw error("SET claim 'aud' must be a non-empty string or an array of non-empty strings", args("aud", setAudienceElement));
		}

		logSuccess("SET claim 'aud' is a string or an array of strings", args("aud", setAudienceElement));

		return env;
	}

	private static boolean isNonEmptyString(JsonElement el) {
		return el.isJsonPrimitive() && el.getAsJsonPrimitive().isString() && !OIDFJSON.getString(el).isEmpty();
	}
}
