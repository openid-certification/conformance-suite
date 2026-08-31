package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class WarnIfPingNotificationWithWrongAuthReqIdWasAccepted extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		Integer statusCode = env.getInteger("client_notification_endpoint_response_http_status");
		if (statusCode == null) {
			throw error("Client notification endpoint response status is missing");
		}

		if (statusCode >= 200 && statusCode < 300) {
			throw error("Client notification endpoint accepted a ping containing an unknown auth_req_id",
				args("http_status", statusCode));
		}

		logSuccess("Client notification endpoint did not accept the ping containing an unknown auth_req_id",
			args("http_status", statusCode));
		return env;
	}
}
