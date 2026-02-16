package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWKUtil;

public class GenerateEntityConfigurationForOPTest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "rp_ec_jwks", strings = "base_url")
	@PostEnvironment(required = "server")
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		JsonObject server = EntityUtils.createBasicClaimsObject(baseUrl, baseUrl);

		server.add("jwks", JWKUtil.toPublicJWKSet(env.getObject("rp_ec_jwks")));

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
