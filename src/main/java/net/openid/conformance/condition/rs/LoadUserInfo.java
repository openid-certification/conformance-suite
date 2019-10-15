package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class LoadUserInfo extends AbstractCondition {

	@Override
	@PostEnvironment(required = "user_info")
	public Environment evaluate(Environment env) {

		JsonObject user = new JsonObject();

		user.addProperty("sub", "user-subject-1234531");
		user.addProperty("name", "Demo T. User");
		user.addProperty("email", "user@example.com");
		user.addProperty("email_verified", false);

		env.putObject("user_info", user);

		logSuccess("Added user information", args("user_info", user));

		return env;
	}

}
