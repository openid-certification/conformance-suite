package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class VerifyBearerTokenHeaderCallback extends AbstractCondition {

	public VerifyBearerTokenHeaderCallback(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment( required = { "notification_callback" }, strings = "client_notification_token")
	public Environment evaluate(Environment env) {

		JsonElement headersCallback = env.getElementFromObject("notification_callback", "headers");

		if (headersCallback == null || !headersCallback.isJsonObject()) {
			throw error("headers in notification_callback must be json and not null");
		}

		JsonObject headers = headersCallback.getAsJsonObject();
		JsonElement authorizationElement = headers.get("authorization");

		if (authorizationElement == null || !authorizationElement.isJsonPrimitive()) {
			throw error("Authorization in headers notification_callback must be primitive and not null.");
		}

		String authorizationExpected = String.format("bearer %s", env.getString("client_notification_token"));
		String authorizationContent = authorizationElement.getAsString();

		if (Strings.isNullOrEmpty(authorizationContent)) {
			throw error("Authorization in headers notification_callback must be not null or empty");
		}

		if (!authorizationContent.equals(authorizationExpected)) {
			throw error("Authorization in headers notification_callback did not meet expected.", args("expected", authorizationExpected, "actual", authorizationContent));
		}

		logSuccess("authorization in headers notification_callback contained client_notification_token.");

		return env;
	}
}
