package net.openid.conformance.condition.rs;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.BrazilConsent;

public class FAPIBrazilValidateCibaConsentLoggedUserName extends AbstractCondition {

	@Override
	@PreEnvironment(required = "new_consent_request")
	public Environment evaluate(Environment env) {
		JsonElement businessEntity = env.getElementFromObject("new_consent_request", "data.businessEntity");
		JsonElement name = env.getElementFromObject("new_consent_request", "data.loggedUser.name");
		if (name == null) {
			if (businessEntity != null) {
				throw error("data.loggedUser.name is required for corporate (PJ) consent requests using CIBA");
			}
			logSuccess("Personal consent request does not require data.loggedUser.name");
			return env;
		}
		if (!name.isJsonPrimitive() || !name.getAsJsonPrimitive().isString()) {
			throw error("data.loggedUser.name must be a string", args("name", name));
		}
		String value = OIDFJSON.getString(name);
		if (businessEntity != null && value.isBlank()) {
			throw error("data.loggedUser.name must not be blank for corporate (PJ) consent requests using CIBA");
		}
		if (value.codePointCount(0, value.length()) > BrazilConsent.MAX_LOGGED_USER_NAME_LENGTH) {
			throw error("data.loggedUser.name must not exceed " + BrazilConsent.MAX_LOGGED_USER_NAME_LENGTH
				+ " characters", args("name", name));
		}
		logSuccess("Consent request contains a valid data.loggedUser.name", args("name", name));
		return env;
	}
}
