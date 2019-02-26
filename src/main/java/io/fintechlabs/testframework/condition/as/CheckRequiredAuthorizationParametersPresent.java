package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

import java.util.ArrayList;
import java.util.List;

public class CheckRequiredAuthorizationParametersPresent extends AbstractCondition {

	public CheckRequiredAuthorizationParametersPresent(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String[] requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request"})
	public Environment evaluate(Environment env) {


		List<String> responses = new ArrayList<>();
		responses.add(env.getString("authorization_endpoint_request", "params.response_type"));
		responses.add(env.getString("authorization_endpoint_request", "params.client_id"));
		responses.add(env.getString("authorization_endpoint_request", "params.redirect_uri"));
		responses.add(env.getString("authorization_endpoint_request", "params.scope"));

		for (String singleResponse : responses) {
			if (Strings.isNullOrEmpty(singleResponse)) {
				throw error("Required parameter value(s) not present in the authorization endpoint request", args("Missing parameter", singleResponse));
			}
		}

		logSuccess("Required parameter values are found outside of the post body", args("parameters", responses));

		return env;
	}
}
