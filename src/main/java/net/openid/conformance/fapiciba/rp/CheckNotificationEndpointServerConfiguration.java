package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckNotificationEndpointServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config", strings = "ciba_mode")
	public Environment evaluate(Environment env) {
		String cibaMode = env.getString("ciba_mode");
		if("ping".equalsIgnoreCase(cibaMode)) {
			String notificationEndpoint = env.getString("config", "client.backchannel_client_notification_endpoint");
			if(!notificationEndpoint.startsWith("https")) {
				throw error("backchannel_client_notification_endpoint must be https", args("backchannel_client_notification_endpoint", notificationEndpoint));
			}
		}
		logSuccess("backchannel_client_notification_endpoint is https");
		return env;
	}

}
