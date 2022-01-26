package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureConsentWasAuthorised extends AbstractJsonAssertingCondition {

	@Override
	@PreEnvironment(strings = "resource_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonElement consentResponse = bodyFrom(env);
		JsonElement statusElement = findByPath(consentResponse, "$.data.status");
		String status = OIDFJSON.getString(statusElement);
		if(!status.equals("AUTHORISED")) {
			throw error("Expected consent to be in the AUTHORISED state after redirect but it was not", args("status", status));
		}
		logSuccess("Consent was in the AUTHORISED state after redirect");
		return env;
	}

}
