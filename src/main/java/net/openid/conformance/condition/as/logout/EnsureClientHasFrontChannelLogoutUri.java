package net.openid.conformance.condition.as.logout;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientHasFrontChannelLogoutUri extends AbstractCondition {


	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		String frontchannelLogoutUri = env.getString("client", "frontchannel_logout_uri");

		if(frontchannelLogoutUri==null || frontchannelLogoutUri.isEmpty()) {
			throw error("frontchannel_logout_uri is not defined for the client");
		}

		logSuccess("frontchannel_logout_uri is set", args("frontchannel_logout_uri", frontchannelLogoutUri));

		return env;

	}

}
