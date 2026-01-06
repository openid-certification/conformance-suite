package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.concurrent.TimeUnit;

public class OIDSSFGetOrWaitForPushRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject pushRequestObject = waitForPushRequestObject(env);
		if (pushRequestObject == null) {
			throw error("Did not receive push request");
		}

		logSuccess("Detected push request object", args("push_request", pushRequestObject));

		return env;
	}

	protected JsonObject waitForPushRequestObject(Environment env) {

		JsonObject pushRequestObject;
		for (int i = 0; i < 5; i++) {
			JsonElement elementFromObject = env.getElementFromObject("ssf", "push_request");

			if (elementFromObject != null) {
				pushRequestObject = elementFromObject.getAsJsonObject();
				if (pushRequestObject != null) {
					log("Found push request object");
					return pushRequestObject;
				}
			}
			log("Waiting for push request object");
			waitSeconds(5);
		}

		return null;
	}

	protected void waitSeconds(int expectedWaitSeconds) {
		logSuccess("Pausing for " + expectedWaitSeconds + " seconds");

		try {
			TimeUnit.SECONDS.sleep(expectedWaitSeconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		logSuccess("Woke up after " + expectedWaitSeconds + " seconds sleep");
	}
}
