package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class GenerateTrustAnchorEntityConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "trust_anchor_entity_identifier")
	@PostEnvironment(required = "trust_anchor")
	public Environment evaluate(Environment env) {

		String trustAnchorEntityIdentifier = env.getString("trust_anchor_entity_identifier");

		JsonObject trustAnchor = new JsonObject();
		trustAnchor.addProperty("iss", trustAnchorEntityIdentifier);
		trustAnchor.addProperty("sub", trustAnchorEntityIdentifier);

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(5 * 60);
		trustAnchor.addProperty("iat", iat.getEpochSecond());
		trustAnchor.addProperty("exp", exp.getEpochSecond());

		trustAnchor.add("jwks", env.getObject("trust_anchor_public_jwks"));

		env.putObject("trust_anchor", trustAnchor);

		logSuccess("Created trust anchor entity configuration", args("trust_anchor", trustAnchor, "entity_identifier", trustAnchorEntityIdentifier));

		return env;
	}

}
