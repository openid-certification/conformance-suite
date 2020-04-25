package net.openid.conformance.condition.as.logout;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureBackChannelLogoutUriResponseStatusCodeIs400 extends AbstractCondition {


	@Override
	@PreEnvironment(required = "backchannel_logout_endpoint_response")
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("backchannel_logout_endpoint_response", "status");

		if(statusCode!=400) {
			throw error("backchannel_logout_uri returned an unexpected response", args("http_status", statusCode));
		}

		logSuccess("backchannel_logout_uri returned http 400 as expected", args("http_status", statusCode));

		return env;

	}

}
