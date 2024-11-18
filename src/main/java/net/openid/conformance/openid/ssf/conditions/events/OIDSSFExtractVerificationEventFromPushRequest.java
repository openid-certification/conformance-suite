package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFExtractVerificationEventFromPushRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject pushRequestObject = env.getElementFromObject("ssf", "push_request").getAsJsonObject();
		String body = OIDFJSON.getString(pushRequestObject.get("body"));

		env.putString("ssf","verification.jwt", body);

		logSuccess("Extracted verification event token from push request", args("jwt", body));

		return env;
	}
}
