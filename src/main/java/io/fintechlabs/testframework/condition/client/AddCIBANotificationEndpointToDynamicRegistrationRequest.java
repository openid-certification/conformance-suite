package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

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
