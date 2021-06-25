package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateConsentWasNotFound extends AbstractCondition {

	@Override
	@PreEnvironment(required = "errored_response", strings = "consent_id")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("errored_response");
		String consentId = env.getString("consent_id");
		int statusCode = OIDFJSON.getInt(response.get("status_code"));
		if(statusCode != 404) {
			error("Was expecting a 404 response but it was actually " + statusCode);
		}
		log("The consent was not found, as expected.", args("consentId", consentId));
		return env;
	}
}
