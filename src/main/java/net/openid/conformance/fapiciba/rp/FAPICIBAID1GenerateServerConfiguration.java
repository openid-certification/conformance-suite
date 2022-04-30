package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPICIBAID1GenerateServerConfiguration extends AbstractCondition {

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
		server.addProperty("authorization_endpoint", baseUrl + "authorize");
		server.addProperty("token_endpoint", baseUrl + "token");
		server.addProperty("jwks_uri", baseUrl + "jwks");
		server.addProperty("registration_endpoint", baseUrl + "register");
		server.addProperty("userinfo_endpoint", baseUrl + "userinfo");
		server.addProperty("backchannel_authentication_endpoint", baseUrl + "backchannel");

		final JsonArray values = new JsonArray();
		values.add("poll");
		values.add("ping");
		server.add("backchannel_token_delivery_modes_supported", values);

		JsonArray algs = new JsonArray();
		algs.add("PS256");
		server.add("backchannel_authentication_request_signing_alg_values_supported", algs);

		// Leaving this here since mtls client authentication and certificate-bound access tokens can be used independently of each other.
		server.addProperty("tls_client_certificate_bound_access_tokens", true);

		server.addProperty("request_parameter_supported", true);

		env.putObject("server", server);

		env.putString("issuer", baseUrl);
		env.putString("discoveryUrl", baseUrl + ".well-known/openid-configuration");

		logSuccess("Created server configuration", args("server", server, "issuer", baseUrl, "discoveryUrl", baseUrl + ".well-known/openid-configuration"));

		return env;
	}

}
