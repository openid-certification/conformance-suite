package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureLoginHintEqualsConsentId extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"consent_id"})
	public Environment evaluate(Environment env) {

		String consentId = env.getString("consent_id");
		JsonElement loginHintElement = env.getElementFromObject("backchannel_request_object", "claims.login_hint");

		if (loginHintElement == null || !loginHintElement.isJsonPrimitive() || !loginHintElement.getAsJsonPrimitive().isString()) {
			throw error("login_hint is missing or not a string", args("login_hint", loginHintElement));
		}

		String loginHint = OIDFJSON.getString(loginHintElement);
		if (!consentId.equals(loginHint)) {
			throw error("login_hint does not match consent_id", args("consent_id", consentId, "login_hint", loginHint));
		}

		logSuccess("login_hint matches consent_id", args("consent_id", consentId, "login_hint", loginHint));
		return env;
	}

}
