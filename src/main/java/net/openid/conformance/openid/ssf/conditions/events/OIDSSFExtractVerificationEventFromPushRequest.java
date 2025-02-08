package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.concurrent.TimeUnit;

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

		JsonObject pushRequestObject;
		for (int i = 0; i < 5; i++) {
			JsonElement elementFromObject = env.getElementFromObject("ssf", "push_request");

			if (elementFromObject != null) {
				pushRequestObject = elementFromObject.getAsJsonObject();
				if (pushRequestObject != null) {
					logSuccess("Found push request object");
					return pushRequestObject;
				}
			}
			log("Waiting for push request object");
			waitSeconds(5);
		}

		return null;
	}

	private void waitSeconds(int expectedWaitSeconds) {
		logSuccess("Pausing for " + expectedWaitSeconds + " seconds");

		try {
			TimeUnit.SECONDS.sleep(expectedWaitSeconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		logSuccess("Woke up after " + expectedWaitSeconds + " seconds sleep");
	}
}
