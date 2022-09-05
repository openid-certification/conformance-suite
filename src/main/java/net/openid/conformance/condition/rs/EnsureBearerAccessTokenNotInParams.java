package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureBearerAccessTokenNotInParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {

		String incomingQuery = env.getString("incoming_request", "query_string_params.access_token");
		String incomingForm = env.getString("incoming_request", "body_form_params.access_token");

		if (!Strings.isNullOrEmpty(incomingQuery)) {
			throw error("Client incorrectly supplied access token in query parameters", args("access_token", incomingQuery));
		}
		if (!Strings.isNullOrEmpty(incomingForm)) {
			throw error("Client incorrectly supplied access token in form parameters", args("access_token", incomingForm));
		}
		logSuccess("Client correctly did not send access token in query parameters or form body");
		return env;
	}

}
