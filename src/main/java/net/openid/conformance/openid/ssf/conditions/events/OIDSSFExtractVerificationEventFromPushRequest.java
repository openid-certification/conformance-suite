package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.client.WaitFor5Seconds;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFExtractVerificationEventFromPushRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject pushRequestObject = waitForPushRequestObject(env);
		if (pushRequestObject == null) {
			throw error("Did not receive push request");
		}

		String body = OIDFJSON.getString(pushRequestObject.get("body"));
		env.putString("ssf", "verification.jwt", body);

		JsonObject headers = pushRequestObject.getAsJsonObject("headers");
		env.putObject("ssf","verification.headers", headers);

		logSuccess("Extracted verification event token from push request", args("jwt", body));

		return env;
	}

	private JsonObject waitForPushRequestObject(Environment env) {
		WaitFor5Seconds wait = new WaitFor5Seconds();

		JsonObject pushRequestObject = null;
		for (int i = 0; i < 5; i++) {
			pushRequestObject = env.getElementFromObject("ssf", "push_request").getAsJsonObject();
			if (pushRequestObject != null) {
				log("Found push request object");
				break;
			}
			log("Waiting for push request object");
			wait.evaluate(env);
		}
		return pushRequestObject;
	}
}
