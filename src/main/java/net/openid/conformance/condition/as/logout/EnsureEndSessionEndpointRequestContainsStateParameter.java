package net.openid.conformance.condition.as.logout;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureEndSessionEndpointRequestContainsStateParameter extends AbstractCondition {


	@Override
	@PreEnvironment(required = "end_session_endpoint_http_request_params")
	public Environment evaluate(Environment env) {

		String state = env.getString("end_session_endpoint_http_request_params", "state");

		if(state==null || state.isEmpty()) {
			throw error("Missing state parameter. end_session_endpoint request must contain a state parameter");
		}

		logSuccess("end_session_endpoint request contains state parameter", args("state", state));

		return env;

	}

}
