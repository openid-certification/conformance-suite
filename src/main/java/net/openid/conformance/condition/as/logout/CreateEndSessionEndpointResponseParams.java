package net.openid.conformance.condition.as.logout;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.Environment;

public class CreateEndSessionEndpointResponseParams extends AbstractCondition {


	@Override
	@PreEnvironment(required = "end_session_endpoint_http_request_params")
	@PostEnvironment(required = "end_session_endpoint_response_params")
	public Environment evaluate(Environment env) {

		String state = env.getString("end_session_endpoint_http_request_params", "state");

		JsonObject responseParams = new JsonObject();
		if(state!=null) {
			responseParams.addProperty("state", state);
		}

		log("Added end_session_endpoint_response_params to environment", args("params", responseParams));

		env.putObject("end_session_endpoint_response_params", responseParams);

		return env;

	}

}
