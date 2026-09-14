package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureRegisteredCIBANotificationEndpointMatches extends AbstractCondition {
	@Override
	@PreEnvironment(required = "client", strings = "notification_uri")
	public Environment evaluate(Environment env) {
		String expected = env.getString("notification_uri");
		if (expected.isBlank()) {
			throw error("The test notification endpoint URI is empty");
		}
		JsonElement registered = env.getElementFromObject("client", "backchannel_client_notification_endpoint");
		if (registered == null || !registered.isJsonPrimitive()
			|| !registered.getAsJsonPrimitive().isString()
			|| !expected.equals(OIDFJSON.getString(registered))) {
			throw error("The registered notification endpoint does not match the endpoint served by this test. "
				+ "Configure the authorization server to retain the requested notification endpoint and rerun the test.",
				args("requested_notification_endpoint", expected,
					"registered_notification_endpoint",
					registered != null && registered.isJsonNull() ? null : registered));
		}
		logSuccess("The registered notification endpoint matches the endpoint served by this test",
			args("notification_endpoint", expected));
		return env;
	}
}
