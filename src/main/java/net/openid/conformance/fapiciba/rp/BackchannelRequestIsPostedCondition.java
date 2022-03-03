package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class BackchannelRequestIsPostedCondition extends AbstractCondition {

	@Override
	@PreEnvironment(required = "backchannel_endpoint_http_request")
	public Environment evaluate(Environment env) {

		String method = env.getString("backchannel_endpoint_http_request", "method");

		if(method == null || !method.equalsIgnoreCase("POST")) {
			throw error("HTTP request method must be 'POST'");
		}

		logSuccess("Backchannel authentication request received using HTTP POST");

		return env;
	}


}
