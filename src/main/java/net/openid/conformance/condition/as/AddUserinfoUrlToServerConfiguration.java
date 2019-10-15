package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddUserinfoUrlToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server", strings = "base_url")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");

		// set off the URLs below with a slash, if needed
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		JsonObject server = env.getObject("server");

		server.addProperty("userinfo_endpoint", baseUrl + "userinfo");

		return env;
	}

}
