package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.Environment;

public class PingClientNotificationEndpointWithWrongAuthReqId extends PingClientNotificationEndpointExpectingClientError {

	@Override
	protected String getAuthReqId(Environment env) {
		return env.getString("auth_req_id") + "-invalid";
	}
}
