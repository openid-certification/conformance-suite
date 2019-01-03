package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class AddRequestToBackchannelAuthenticationEndpointRequest extends AbstractCondition {

	public AddRequestToBackchannelAuthenticationEndpointRequest(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = "backchannel_authentication_endpoint_request_form_parameters", strings = "request_object" )
	@PostEnvironment(required = "backchannel_authentication_endpoint_request_form_parameters")
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("backchannel_authentication_endpoint_request_form_parameters");

		o.addProperty("request", env.getString("request_object"));

		env.putObject("backchannel_authentication_endpoint_request_form_parameters", o);

		log(o);

		return env;

	}

}
