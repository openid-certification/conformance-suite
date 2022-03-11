package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VerifyPingHttpResponseStatusCodeIs204 extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		int statusCode = env.getInteger("client_notification_endpoint_response_http_status");

		if(statusCode != 204) {
			throw error("The client_notification_endpoint should return http status code 204", args("http_status", statusCode));
		}

		logSuccess("client_notification_endpoint returned the expected http status", args("http_status", statusCode));

		return env;
	}

}
