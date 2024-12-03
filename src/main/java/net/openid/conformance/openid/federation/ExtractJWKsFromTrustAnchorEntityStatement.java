package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractJWKsFromTrustAnchorEntityStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "trust_anchor_entity_statement_jwt")
	@PostEnvironment(required = { "server_jwks" })
	public Environment evaluate(Environment env) {

		JsonObject jwks = env.getElementFromObject("trust_anchor_entity_statement_jwt", "claims.jwks").getAsJsonObject();
		env.putObject("server_jwks", jwks);

		logSuccess("Extracted JWKs from entity statement", args("server_jwks", jwks));

		return env;
	}

}
