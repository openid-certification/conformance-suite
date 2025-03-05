package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class GenerateEntityConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		JsonObject server = new JsonObject();
		server.addProperty("iss", baseUrl);
		server.addProperty("sub", baseUrl);

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(5 * 60);
		server.addProperty("iat", iat.getEpochSecond());
		server.addProperty("exp", exp.getEpochSecond());

		server.add("jwks", env.getObject("server_public_jwks"));

		env.putObject("server", server);

		logSuccess("Created entity configuration", args("server", server, "entity_identifier", baseUrl));

		return env;
	}

}
