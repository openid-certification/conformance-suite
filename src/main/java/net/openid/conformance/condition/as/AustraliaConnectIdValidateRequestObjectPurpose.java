package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AustraliaConnectIdValidateRequestObjectPurpose extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {
		JsonElement purposeElement = env.getElementFromObject("authorization_request_object", "claims.purpose");

		if (purposeElement == null) {
			throw error("'purpose' claim in request object is required but not present");
		}
		if (!purposeElement.isJsonPrimitive() || !purposeElement.getAsJsonPrimitive().isString()) {
			throw error("'purpose' claim in request object is not a string");
		}
		String purpose = OIDFJSON.getString(purposeElement);
		if (purpose.length() < 3) {
			throw error("'purpose' claim in request object is shorter than 3 characters");
		}
		if (purpose.length() > 300) {
			throw error("'purpose' claim in request object is longer than 300 characters");
		}

		logSuccess("'purpose' claim in request object is present and an acceptable length");

		return env;
	}
}