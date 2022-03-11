package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VerifyPingHttpResponseStatusCodeIsNot3XX extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("client_notification_endpoint_response_http_status");

		if(statusCode >= 300 && statusCode < 400) {
			throw error("client_notification_endpoint must not return a 3XX status code", args("http_status", statusCode));
		}

		logSuccess("client_notification_endpoint returned the expected http status", args("http_status", statusCode));

		return env;
	}

}
