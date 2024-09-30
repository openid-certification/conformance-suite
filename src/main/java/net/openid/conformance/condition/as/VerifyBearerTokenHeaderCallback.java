package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VerifyBearerTokenHeaderCallback extends AbstractCondition {

	@Override
	@PreEnvironment( required = { "notification_callback" }, strings = "client_notification_token")
	public Environment evaluate(Environment env) {

		JsonElement headersCallback = env.getElementFromObject("notification_callback", "headers");

		if (headersCallback == null || !headersCallback.isJsonObject()) {
			throw error("header in notification callback is missing or empty");
		}

		JsonObject headers = headersCallback.getAsJsonObject();
		JsonElement authorizationElement = headers.get("authorization");

		if (authorizationElement == null || !authorizationElement.isJsonPrimitive()) {
			throw error("'Authorization' header in notification callback must be primitive and not null.");
		}


		String authorizationContent = OIDFJSON.getString(authorizationElement);
		if (Strings.isNullOrEmpty(authorizationContent)) {
			throw error("'Authorization' header in notification callback is missing or empty");
		}

		String[] authorizationContentElements = authorizationContent.split("( )+");

		if (authorizationContentElements.length == 2
			&& authorizationContentElements[0].equalsIgnoreCase("Bearer")
			&& authorizationContentElements[1].equals(env.getString("client_notification_token"))) {
			logSuccess("'Authorization' header in notification callback contained client_notification_token.");
		} else {
			String authorizationExpected = "bearer %s".formatted(env.getString("client_notification_token"));
			throw error("The value of the 'Authorization' header in the notification callback is not correct.", args("expected", authorizationExpected, "actual", authorizationContent));
		}

		return env;
	}
}
