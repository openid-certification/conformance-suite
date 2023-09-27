package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GenerateServerConfigurationMTLS extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"base_url", "base_mtls_url"})
	@PostEnvironment(required = "server", strings = { "issuer", "discoveryUrl" })
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");
		String baseMtlsUrl = env.getString("base_mtls_url");


		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		if (baseMtlsUrl.isEmpty()) {
			throw error("Base MTLS URL is empty");
		}

		// set off the URLs below with a slash, if needed
		if (!baseUrl.endsWith("/")) {
			baseUrl = baseUrl + "/";
		}

		if (!baseMtlsUrl.endsWith("/")) {
			baseMtlsUrl = baseMtlsUrl + "/";
		}

		// create a base server configuration object based on the base URL
		JsonObject server = new JsonObject();
		JsonObject mtlsAliases = new JsonObject();

		server.addProperty("issuer", baseUrl);
		server.addProperty("authorization_endpoint", baseUrl + "authorize");

		server.addProperty("token_endpoint", baseUrl + "token");
		mtlsAliases.addProperty("token_endpoint", baseMtlsUrl + "token");

		server.addProperty("jwks_uri", baseUrl + "jwks");

		server.addProperty("registration_endpoint", baseUrl + "register"); // TODO: should this be pulled into an optional mix-in?
		mtlsAliases.addProperty("registration_endpoint", baseMtlsUrl + "register");

		server.addProperty("userinfo_endpoint", baseUrl + "userinfo"); // TODO: should this be pulled into an optional mix-in?
		mtlsAliases.addProperty("userinfo_endpoint", baseMtlsUrl + "userinfo");

		server.add("mtls_endpoint_aliases", mtlsAliases);

		// add this as the server configuration
		env.putObject("server", server);

		env.putString("issuer", baseUrl);
		env.putString("discoveryUrl", baseUrl + ".well-known/openid-configuration");

		logSuccess("Created server configuration", args("server", server, "issuer", baseUrl, "discoveryUrl", baseUrl + ".well-known/openid-configuration"));

		return env;

	}

}
