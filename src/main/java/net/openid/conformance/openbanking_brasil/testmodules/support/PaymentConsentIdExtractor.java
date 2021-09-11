package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class PaymentConsentIdExtractor extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_response")
	@PostEnvironment(strings = "consent_id")
	public Environment evaluate(Environment env) {
		JsonObject consent = env.getObject("consent_endpoint_response");
		JsonObject data = consent.getAsJsonObject("data");
		String consentId = OIDFJSON.getString(data.get("consentId"));
		env.putString("consent_id", consentId);
		logSuccess("Found consent id", args("consentId", consentId));
		return env;
	}

}
