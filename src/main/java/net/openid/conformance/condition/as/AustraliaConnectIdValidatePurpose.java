package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class AustraliaConnectIdValidatePurpose extends AbstractCondition {

	@Override
	@PreEnvironment(required = {CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY})
	public Environment evaluate(Environment env) {
		JsonObject authReq = env.getObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY);
		JsonElement purposeElement = env.getElementFromObject(CreateEffectiveAuthorizationPARRequestParameters.ENV_KEY, "purpose");
		if (purposeElement == null) {
			throw error("'purpose' claim in authorization request is required but not present", authReq);
		}
		if (!purposeElement.isJsonPrimitive() || !purposeElement.getAsJsonPrimitive().isString()) {
			throw error("'purpose' claim in authorization request is not a string", authReq);
		}
		String purpose = OIDFJSON.getString(purposeElement);
		if (purpose.length() < 3) {
			throw error("'purpose' claim in authorization request is shorter than 3 characters", authReq);
		}
		if (purpose.length() > 300) {
			throw error("'purpose' claim in authorization request is longer than 300 characters", authReq);
		}

		logSuccess("'purpose' claim in authorization request is present and an acceptable length");

		return env;
	}
}
