package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;

public class FAPICIBAEnsureRegistrationRequestNotificationEndpointIsHttps extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {
		JsonElement endpointElement = env.getElementFromObject(
			"dynamic_registration_request", "backchannel_client_notification_endpoint");
		if (endpointElement == null
			|| !endpointElement.isJsonPrimitive()
			|| !endpointElement.getAsJsonPrimitive().isString()) {
			throw error("backchannel_client_notification_endpoint must be an HTTPS URI string",
				args("backchannel_client_notification_endpoint", endpointElement));
		}

		String endpoint = OIDFJSON.getString(endpointElement);
		URI endpointUri;
		try {
			endpointUri = URI.create(endpoint);
		} catch (IllegalArgumentException invalidUri) {
			throw error("backchannel_client_notification_endpoint is not a valid URI",
				invalidUri, args("backchannel_client_notification_endpoint", endpoint));
		}
		if (!endpointUri.isAbsolute()
			|| !"https".equalsIgnoreCase(endpointUri.getScheme())
			|| endpointUri.getHost() == null) {
			throw error("backchannel_client_notification_endpoint must be an absolute HTTPS URI",
				args("backchannel_client_notification_endpoint", endpoint));
		}

		logSuccess("Registration request contains an HTTPS CIBA notification endpoint",
			args("backchannel_client_notification_endpoint", endpoint));
		return env;
	}
}
