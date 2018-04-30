package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author srmoore
 *
 */
public class CreateDynamicRegistrationRequest extends AbstractCondition {

	public CreateDynamicRegistrationRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements){
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PostEnvironment(required = "dynamic_registration_request")
	public Environment evaluate(Environment env) {

		// create an empty JSON to act as the registration request
		JsonObject dynamicRegistrationRequest = new JsonObject();

		// get the specified "client_name" from the client object if there is one.
		String clientName = env.getString("client_name");

		if(Strings.isNullOrEmpty(clientName)){
			clientName = this.getTestId();
		} else {
			clientName = clientName + " " + this.getTestId();
		}

		dynamicRegistrationRequest.addProperty("client_name", clientName);
		env.put("dynamic_registration_request", dynamicRegistrationRequest);

		logSuccess("Created dynamic registration request", dynamicRegistrationRequest);

		return env;
	}
}
