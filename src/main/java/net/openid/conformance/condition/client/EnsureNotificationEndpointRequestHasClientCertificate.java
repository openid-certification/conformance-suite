package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class EnsureNotificationEndpointRequestHasClientCertificate extends AbstractCondition {

	@Override
	@PreEnvironment(required = "notification_callback")
	public Environment evaluate(Environment env) {
		if (!hasClientCertificate(env.getObject("notification_callback"))) {
			throw error("Client notification endpoint request did not include a mutual TLS client certificate");
		}

		logSuccess("Client notification endpoint request included a mutual TLS client certificate");
		return env;
	}

	public static boolean hasClientCertificate(JsonObject request) {
		// The TLS proxy supplies this header. This checks certificate presence, not certificate trust
		// or binding to an authorization server identity; those require separate validation.
		JsonElement headers = request.get("headers");
		if (headers == null || !headers.isJsonObject()) {
			return false;
		}
		JsonElement value = headers.getAsJsonObject().get("x-ssl-cert");
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
			return false;
		}
		String certificate = OIDFJSON.getString(value);
		return !certificate.isBlank() && !"(null)".equals(certificate);
	}
}
