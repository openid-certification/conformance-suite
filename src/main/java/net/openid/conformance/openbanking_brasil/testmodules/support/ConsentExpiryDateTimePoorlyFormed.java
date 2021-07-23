package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ConsentExpiryDateTimePoorlyFormed extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_request")
	public Environment evaluate(Environment env) {

		String badDateTime = "2021-13-32T10:00:00-05:00";
		JsonObject consentRequest = env.getObject("consent_endpoint_request");
		JsonObject data = consentRequest.getAsJsonObject("data");

		data.addProperty("expirationDateTime", badDateTime);
		logSuccess("Set expiry date to be poorly formed", args("expiry", badDateTime));
		return env;
	}
}
