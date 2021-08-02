package net.openid.conformance.condition.as.logout;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureBackChannelLogoutUriResponseStatusCodeIs200 extends AbstractCondition {


	@Override
	@PreEnvironment(required = "backchannel_logout_endpoint_response")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("backchannel_logout_endpoint_response", "status");

		if(statusCode!=200) {
			throw error("backchannel_logout_uri returned an unexpected http status", args("http_status", statusCode));
		}

		logSuccess("backchannel_logout_uri returned the expected http status", args("http_status", statusCode));

		return env;

	}

}
