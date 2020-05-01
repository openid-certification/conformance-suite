package net.openid.conformance.condition.as.logout;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureClientHasAtLeastOneOfBackOrFrontChannelLogoutUri extends AbstractCondition {


	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		String backChannelLogoutUri = env.getString("client", "backchannel_logout_uri");
		String frontChannelLogoutUri = env.getString("client", "frontchannel_logout_uri");

		boolean hasBackChannel=false;
		boolean hasFrontChannel=false;

		if(frontChannelLogoutUri!=null && !frontChannelLogoutUri.isEmpty()) {
			hasFrontChannel=true;
		}

		if(backChannelLogoutUri!=null && !backChannelLogoutUri.isEmpty()) {
			hasBackChannel=true;
		}
		if(hasBackChannel||hasFrontChannel) {
			logSuccess("Client has either backchannel_logout_uri or frontchannel_logout_uri (or both) set",
						args("backchannel_logout_uri", backChannelLogoutUri,
								"frontchannel_logout_uri", frontChannelLogoutUri));
		} else {
			throw error("At least one of backchannel_logout_uri or frontchannel_logout_uri is required");
		}

		return env;

	}

}
