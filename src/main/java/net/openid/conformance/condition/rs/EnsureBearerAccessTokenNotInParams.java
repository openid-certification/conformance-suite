package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureBearerAccessTokenNotInParams extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	public Environment evaluate(Environment env) {
		String incoming = env.getString("incoming_request", "params.access_token");

		if (!Strings.isNullOrEmpty(incoming)) {
			throw error("Client incorrectly supplied access token in query parameters or form body", args("access_token", incoming));
		} else {
			logSuccess("Client correctly did not send access token in query parameters or form body");
			return env;
		}
	}

}
