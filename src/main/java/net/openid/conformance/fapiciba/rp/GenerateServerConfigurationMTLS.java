package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.runner.TestDispatcher;
import net.openid.conformance.testmodule.Environment;

public class GenerateServerConfigurationMTLS extends AbstractCondition {

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

		// FIXME: we should inject a base_url_mtls as well instead of having to do this hack
		String baseUrlMtls = baseUrl.replaceFirst(TestDispatcher.TEST_PATH, TestDispatcher.TEST_MTLS_PATH);

		// create a base server configuration object based on the base URL
		JsonObject server = new JsonObject();

		server.addProperty("issuer", baseUrl);
		server.addProperty("authorization_endpoint", baseUrl + "authorize");
		server.addProperty("token_endpoint", baseUrlMtls + "token");
		server.addProperty("jwks_uri", baseUrl + "jwks");

		server.addProperty("registration_endpoint", baseUrl + "register"); // TODO: should this be pulled into an optional mix-in?
		server.addProperty("userinfo_endpoint", baseUrl + "userinfo"); // TODO: should this be pulled into an optional mix-in?
		server.addProperty("backchannel_authentication_endpoint", baseUrl + "backchannel");

		final JsonArray values = new JsonArray();
		values.add("poll");
		values.add("ping");
		server.add("backchannel_token_delivery_modes_supported", values);

		// add this as the server configuration
		env.putObject("server", server);

		env.putString("issuer", baseUrl);
		env.putString("discoveryUrl", baseUrl + ".well-known/openid-configuration");

		logSuccess("Created server configuration", args("server", server, "issuer", baseUrl, "discoveryUrl", baseUrl + ".well-known/openid-configuration"));

		return env;

	}

}
