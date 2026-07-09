package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetRequestObjectLoginHintToInvalidBrazilConsentId extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims", strings = "consent_id")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {
		String consentId = env.getString("consent_id");
		String invalidConsentId = createInvalidConsentId(consentId);
		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.addProperty("login_hint", invalidConsentId);
		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Set login_hint in request object claims to an invalid Brazil consent id",
			args("valid_consent_id", consentId, "login_hint", invalidConsentId, "request_object_claims", requestObjectClaims));

		return env;
	}

	private String createInvalidConsentId(String consentId) {
		if (consentId.isEmpty()) {
			throw error("consent_id must not be empty");
		}

		char replacement = consentId.charAt(consentId.length() - 1) == '0' ? '1' : '0';
		return consentId.substring(0, consentId.length() - 1) + replacement;
	}
}
