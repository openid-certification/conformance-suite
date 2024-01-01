package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GenerateServerConfiguration extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "server", strings = { "issuer", "discoveryUrl" })
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		// set off the URLs below with a slash, if needed
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		JsonObject server = new JsonObject();

		server.addProperty("issuer", baseUrl);
		server.addProperty("token_endpoint", baseUrl + "token");
		server.addProperty("jwks_uri", baseUrl + "jwks");
		server.addProperty("userinfo_endpoint", baseUrl + "userinfo");
		server.addProperty("backchannel_authentication_endpoint", baseUrl + "backchannel");

		JsonArray grantTypes = new JsonArray();
		grantTypes.add("client_credentials");
		grantTypes.add("refresh_token");
		grantTypes.add("urn:openid:params:grant-type:ciba");
		server.add("grant_types_supported", grantTypes);

		final JsonArray cibaModes = new JsonArray();
		server.add("backchannel_token_delivery_modes_supported", cibaModes);

		server.addProperty("request_parameter_supported", true);

		env.putObject("server", server);

		env.putString("issuer", baseUrl);
		env.putString("discoveryUrl", baseUrl + ".well-known/openid-configuration");

		logSuccess("Created server configuration", args("server", server, "issuer", baseUrl, "discoveryUrl", baseUrl + ".well-known/openid-configuration"));

		return env;
	}

}
