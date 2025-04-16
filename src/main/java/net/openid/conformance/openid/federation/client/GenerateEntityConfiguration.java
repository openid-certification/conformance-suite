package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.federation.EntityUtils;
import net.openid.conformance.testmodule.Environment;

public class GenerateEntityConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		JsonObject server = EntityUtils.createBasicClaimsObject(baseUrl, baseUrl);

		server.add("jwks", env.getObject("server_public_jwks"));

		JsonElement authorityHintsElement = env.getElementFromObject("config", "federation.authority_hints");
		if (authorityHintsElement != null) {
			if (!authorityHintsElement.isJsonArray()) {
				throw error("authority_hints must be an array of strings");
			}
			JsonArray authorityHints = authorityHintsElement.getAsJsonArray();
			server.add("authority_hints", authorityHints);
		}

		server.addProperty("request_uri_parameter_supported", false);

		env.putObject("server", server);

		logSuccess("Created entity configuration", args("server", server, "entity_identifier", baseUrl));

		return env;
	}

}
