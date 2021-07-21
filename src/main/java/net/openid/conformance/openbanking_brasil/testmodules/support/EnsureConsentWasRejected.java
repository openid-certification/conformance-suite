package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureConsentWasRejected extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject consentResponse = bodyFrom(env);
		JsonElement statusElement = findByPath(consentResponse, "$.data.status");
		String status = OIDFJSON.getString(statusElement);
		if (!status.equals("REJECTED")) {
			throw error("Expected consent to be in the REJECTED state after redirect but it was not", args("status", status));
		}
		logSuccess("Consent was in the REJECTED state after redirect");
		return env;
	}
}
