package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureConsentWasRejected extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement consentResponse = bodyFrom(env);
		if (!consentResponse.getAsJsonObject().entrySet().isEmpty()) {
			JsonElement statusElement = findByPath(consentResponse, "$.data.status");
			String status = OIDFJSON.getString(statusElement);
			if (!status.equals("REJECTED")) {
				throw error("Expected consent to be in the REJECTED state after redirect but it was not", args("status", status));
			}
			logSuccess("Consent was in the REJECTED state after redirect");
			return env;
		}
		throw error("Expected consent to be in the REJECTED state after redirect but instead the consent could not be found");
	}
}
