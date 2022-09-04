package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class LoadUserInfo extends AbstractCondition {

	@Override
	@PostEnvironment(required = "user_info")
	public Environment evaluate(Environment env) {

		String[] claims = {"sub", "name", "email", "email_verified"};
		JsonObject user = OIDCCLoadUserInfo.getUserInfoClaimsValues(claims);
		env.putObject("user_info", user);

		logSuccess("Added user information", args("user_info", user));

		return env;
	}

}
