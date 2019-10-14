package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckNotificationCallbackOnlyAuthReqId extends AbstractCondition {

	private final String keyExpected = "auth_req_id";

	@Override
	@PreEnvironment( required = { "notification_callback" })
	public Environment evaluate(Environment env) {
		JsonElement bodyCallback = env.getElementFromObject("notification_callback", "body_json");

		if (bodyCallback == null || !bodyCallback.isJsonObject()) {
			throw error("body received in notification callback must be JSON");
		}

		JsonObject bodyJson = bodyCallback.getAsJsonObject();

		int keySize = bodyJson.size();

		if (keySize == 0) {
			throw error("body received in notification callback was empty");
		} else if (keySize > 1 || !bodyJson.keySet().contains(keyExpected)) {
			throw error("body received in notification callback did not contain only auth_req_id", args("actual", bodyJson));
		}

		logSuccess("body received in notification callback contained only auth_req_id", args("body", bodyCallback));

		return env;
	}
}
