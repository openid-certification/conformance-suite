package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveStateFromEndSessionEndpointResponseParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = "end_session_endpoint_response_params")
	@PostEnvironment(required = "end_session_endpoint_response_params")
	public Environment evaluate(Environment env) {

		JsonObject responseParams = env.getObject("end_session_endpoint_response_params");
		if(responseParams.has("state")) {
			responseParams.remove("state");
			log("Removed state from end_session_endpoint response parameters", args("params", responseParams));
			env.putObject("end_session_endpoint_response_params", responseParams);
		} else {
			log("end_session_endpoint response parameters does not contain a state parameter", args("params", responseParams));
		}

		return env;

	}

}
