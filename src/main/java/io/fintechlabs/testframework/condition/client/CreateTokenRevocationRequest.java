package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateTokenRevocationRequest extends AbstractCondition {

	public CreateTokenRevocationRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "access_token")
	@PostEnvironment(required = "revocation_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {
		String accessToken = env.getString("access_token","value");
		if (Strings.isNullOrEmpty(accessToken)){
			throw error ("No access_token value found");
		}
		JsonObject o = new JsonObject();
		o.addProperty("token", accessToken);

		env.putObject("revocation_endpoint_request_form_parameters", o);
		logSuccess(o);

		return env;
	}
}
