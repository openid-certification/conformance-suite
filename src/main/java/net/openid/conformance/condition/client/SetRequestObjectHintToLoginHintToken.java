package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetRequestObjectHintToLoginHintToken extends AbstractCondition {

	public static final String UNSUPPORTED_LOGIN_HINT_TOKEN = "unsupported-login-hint-token";

	@Override
	@PreEnvironment(required = "request_object_claims")
	@PostEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {
		JsonObject requestObjectClaims = env.getObject("request_object_claims");
		requestObjectClaims.remove("login_hint");
		requestObjectClaims.addProperty("login_hint_token", UNSUPPORTED_LOGIN_HINT_TOKEN);
		env.putObject("request_object_claims", requestObjectClaims);

		logSuccess("Replaced login_hint in request object claims with login_hint_token",
			args("login_hint_token", UNSUPPORTED_LOGIN_HINT_TOKEN, "request_object_claims", requestObjectClaims));

		return env;
	}
}
