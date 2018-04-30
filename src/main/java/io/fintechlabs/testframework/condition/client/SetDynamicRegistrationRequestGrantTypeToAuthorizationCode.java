package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author srmoore
 */
public class SetDynamicRegistrationRequestGrantTypeToAuthorizationCode extends AbstractCondition {

	public SetDynamicRegistrationRequestGrantTypeToAuthorizationCode(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = {"dynamic_registration_request"})
	public Environment evaluate(Environment env) {

		if (!env.containsObj("dynamic_registration_request")){
			throw error("No dynamic registration request object found");
		}
		JsonObject dynamicRegistrationRequest = env.get("dynamic_registration_request");
		dynamicRegistrationRequest.addProperty("grant_type","authroization_code");
		env.put("dynamic_registration_request", dynamicRegistrationRequest);

		return env;
	}
}
