package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.Environment;

public class PingClientNotificationEndpointWithBadBearerToken extends PingClientNotificationEndpointExpectingClientError {

	@Override
	protected String getBearerToken(Environment env) {
		return env.getString("client_notification_token") + "1";
	}
}
