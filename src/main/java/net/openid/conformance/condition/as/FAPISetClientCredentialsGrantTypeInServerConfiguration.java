package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPISetClientCredentialsGrantTypeInServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"server"})
	@PostEnvironment(required = {"server"})
	public Environment evaluate(Environment env) {
		JsonArray grantTypes = new JsonArray();
		grantTypes.add("client_credentials");
		JsonObject server = env.getObject("server");
		server.add("grant_types_supported", grantTypes);

		log("Successfully set grant_types_supported", args("server", server));
		return env;
	}

}
