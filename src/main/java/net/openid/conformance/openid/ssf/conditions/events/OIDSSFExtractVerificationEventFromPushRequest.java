package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFExtractVerificationEventFromPushRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonElement pushRequestObjectEl = env.getElementFromObject("ssf", "push_request");
		if (pushRequestObjectEl == null) {
			throw error("Did not receive push request");
		}

		JsonObject pushRequestObject = pushRequestObjectEl.getAsJsonObject();

		String body = OIDFJSON.getString(pushRequestObject.get("body"));
		env.putString("ssf", "verification.jwt", body);

		JsonObject headers = pushRequestObject.getAsJsonObject("headers");
		env.putObject("ssf","verification.headers", headers);

		logSuccess("Extracted verification event token from push request", args("jwt", body));

		return env;
	}
}
