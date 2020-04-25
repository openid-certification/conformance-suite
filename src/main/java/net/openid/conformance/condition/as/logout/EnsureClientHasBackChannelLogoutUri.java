package net.openid.conformance.condition.as.logout;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientHasBackChannelLogoutUri extends AbstractCondition {


	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		String uri = env.getString("client", "backchannel_logout_uri");

		if(uri==null || uri.isEmpty()) {
			throw error("backchannel_logout_uri is not defined for the client");
		}

		logSuccess("backchannel_logout_uri is set", args("backchannel_logout_uri", uri));

		return env;

	}

}
