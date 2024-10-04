package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openid.ssf.variant.SsfDeliveryMode;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class OIDSSFCheckVerificationAuthorizationHeader extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject deliveryObject = env.getElementFromObject("ssf", "delivery").getAsJsonObject();
		String deliveryMethod = OIDFJSON.getString(deliveryObject.get("delivery_method"));
		if (!SsfDeliveryMode.PUSH.getAlias().equals(deliveryMethod)) {
			log("Only relevant for PUSH delivery, skipping check");
			return env;
		}

		JsonElement expectedAuthorizationHeaderEl = env.getElementFromObject("config", "ssf.transmitter.push_endpoint_authorization_header");
		if (expectedAuthorizationHeaderEl == null) {
			log("No authorization header expected, skipping check");
			return env;
		}

		String expectedAuthorizationHeader = OIDFJSON.getString(expectedAuthorizationHeaderEl);

		JsonObject headers = env.getElementFromObject("ssf", "verification.headers").getAsJsonObject();
		if (!headers.has("authorization")) {
			logFailure("Authorization header missing", args("expected_authorization_header", expectedAuthorizationHeader, "headers", headers));
			return env;
		}

		String actualAuthorizationHeader = OIDFJSON.getString(headers.get("authorization"));
		if (!expectedAuthorizationHeader.equals(actualAuthorizationHeader)) {
			logFailure("Authorization header mismatch", args("expected_authorization_header", expectedAuthorizationHeader, "actual_authorization_header", actualAuthorizationHeader));
			return env;
		}

		logSuccess("Received expected authorization header", args("expected_authorization_header", expectedAuthorizationHeader, "actual_authorization_header", actualAuthorizationHeader));

		return env;
	}
}
