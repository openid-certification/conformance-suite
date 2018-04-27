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
 *
 */
public class CreateDynamicRegistrationRequestFromClientInformation extends AbstractCondition {

	public CreateDynamicRegistrationRequestFromClientInformation(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements){
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "client", strings = {"redirect_uri", "client_name"})
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		if (!env.containsObj("client")) {
			throw error("Couldn't find client configuration");
		}

		String clientName = env.getString("client_name");

		if(Strings.isNullOrEmpty(clientName)){
			throw error("Couldn't find client name");
		}

		String redirectUri = env.getString("redirect_uri");

		if (Strings.isNullOrEmpty(redirectUri)) {
			throw error("Couldn't find redirect URI");
		}

		JsonObject dynamicRegistrationRequest = new JsonObject();
		JsonArray redirectUris = new JsonArray();
		redirectUris.add(redirectUri);
		dynamicRegistrationRequest.add("redirect_uris", redirectUris);
		dynamicRegistrationRequest.addProperty("client_name", clientName);

		env.put("dynamic_registration_request", dynamicRegistrationRequest);

		logSuccess("Created dynamic registration request", dynamicRegistrationRequest);

		return env;
	}
}
