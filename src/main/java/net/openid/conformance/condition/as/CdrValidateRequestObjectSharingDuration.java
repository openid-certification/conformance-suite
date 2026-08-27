package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CdrValidateRequestObjectSharingDuration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_request_object")
	public Environment evaluate(Environment env) {

		JsonElement sharingDuration = env.getElementFromObject("authorization_request_object", "claims.claims.sharing_duration");

		if (sharingDuration == null) {
			logSuccess("Request object does not contain a sharing_duration claim; once off access will be assumed");
			return env;
		}

		if (!sharingDuration.isJsonPrimitive() || !sharingDuration.getAsJsonPrimitive().isNumber()) {
			throw error("The sharing_duration claim in the request object is not a number", args("sharing_duration", sharingDuration));
		}

		logSuccess("Request object contains a numeric sharing_duration claim", args("sharing_duration", OIDFJSON.getNumber(sharingDuration.getAsJsonPrimitive())));

		return env;
	}

}
