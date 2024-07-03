package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddAudValueAsArrayToPaymentsConsentResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client_certificate_subject", "consent_response"})
	@PostEnvironment(required = "consent_response")
	public Environment evaluate(Environment env) {

		String aud = env.getString("client_certificate_subject", "ou");

		JsonArray audArray = new JsonArray();
		audArray.add(aud);

		JsonObject consentResponse = env.getObject("consent_response");
		consentResponse.add("aud", audArray);
		env.putObject("consent_response", consentResponse);

		logSuccess("Added the aud value as an array to the payment consent response", args("consent_response", consentResponse, "aud", audArray));

		return env;
	}

}
