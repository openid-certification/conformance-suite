package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddCodeChallengeMethodToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonArray data = new JsonArray();
		data.add("S256");

		JsonObject server = env.getObject("server");
		server.add("code_challenge_methods_supported", data);

		logSuccess("Added S256 as supported code challenge method", args ("code_challenge_methods_supported", data));

		return env;
	}
}
