package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractJWKsFromEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "entity_statement_body")
	@PostEnvironment(required = {"server_jwks" })
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.getElementFromObject("entity_statement_body", "jwks").getAsJsonObject();
		env.putObject("server_jwks", jwks);

		return env;
	}

}
