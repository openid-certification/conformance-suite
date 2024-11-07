package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddSupportedAuthorizationTypesToServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server", "config"})
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		JsonElement rarTypeValues = env.getElementFromObject("config", "resource.authorization_details_types_supported");

		JsonObject server = env.getObject("server");
		server.add("authorization_details_types_supported", rarTypeValues);

		logSuccess("Added 'authorization_details_types_supported' to server metadata", args("value", rarTypeValues));

		return env;
	}
}
