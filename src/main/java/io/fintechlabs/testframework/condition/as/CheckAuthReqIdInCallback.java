package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckAuthReqIdInCallback extends AbstractCondition {

	public CheckAuthReqIdInCallback(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "backchannel_authentication_endpoint_response"})
	public Environment evaluate(Environment env) {

		JsonObject o = env.getObject("notification_callback");
		log("notification_callback contents", o);

		final String authReqId = env.getString("backchannel_authentication_endpoint_response", "auth_req_id");
		if (Strings.isNullOrEmpty(authReqId)) {
			throw error("auth_req_id missing from backchannel_authentication_endpoint_response");
		}

		final String authReqIdFromNotification = env.getString("notification_callback", "body_json.auth_req_id");
		if (Strings.isNullOrEmpty(authReqIdFromNotification)) {
			throw error("auth_req_id missing from CIBA callback");
		}

		if (!authReqId.equals(authReqIdFromNotification)) {
			throw error("auth_req_id does not match",
				args("auth_endpoint", authReqId, "notification", authReqIdFromNotification));
		}

		logSuccess("auth_req_id valued received in callback is correct");

		return env;

	}

}
