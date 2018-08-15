package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author srmoore
 */
public class AddRedirectUriToDynamicRegistrationRequest extends AbstractCondition {

	public AddRedirectUriToDynamicRegistrationRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "dynamic_registration_request", strings = "redirect_uri")
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("dynamic_registration_request")) {
			throw error("No dynamic registration request found");
		}

		JsonObject dynamicRegistrationRequest = env.getObject("dynamic_registration_request");

		String redirectUri = env.getString("redirect_uri");
		if (Strings.isNullOrEmpty(redirectUri)) {
			throw error("No redirect_uri found");
		}

		JsonArray redirectUris = new JsonArray();
		redirectUris.add(redirectUri);
		dynamicRegistrationRequest.add("redirect_uris", redirectUris);

		env.put("dynamic_registration_request", dynamicRegistrationRequest);

		return env;
	}
}
