package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCIBANotificationEndpointToDynamicRegistrationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "dynamic_registration_request", strings = "notification_uri")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		String notificationUri = env.getString("notification_uri");
		if (Strings.isNullOrEmpty(notificationUri)) {
			throw error("No notification_uri found");
		}

		dynamicRegistrationRequest.addProperty("backchannel_client_notification_endpoint", notificationUri);

		env.putObject("dynamic_registration_request", dynamicRegistrationRequest);

		log("Added '"+notificationUri+"' to dynamic registration request", args("dynamic_registration_request", dynamicRegistrationRequest));

		return env;
	}
}
