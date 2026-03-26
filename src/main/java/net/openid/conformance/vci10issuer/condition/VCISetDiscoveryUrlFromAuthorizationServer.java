package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * After VCI credential issuer metadata has been fetched and an OAuth authorization server
 * selected, this condition sets discoveryIssuer and discoveryUrl on the test configuration
 * so that the standard discovery endpoint checks can validate the authorization server metadata.
 */
public class VCISetDiscoveryUrlFromAuthorizationServer extends AbstractCondition {

	@PreEnvironment(required = {"server", "config"})
	@Override
	public Environment evaluate(Environment env) {
		String issuer = env.getString("server", "issuer");
		if (issuer == null) {
			throw error("Authorization server metadata does not contain an 'issuer' field");
		}

		JsonObject config = env.getObject("config");
		JsonObject serverConfig = config.getAsJsonObject("server");
		if (serverConfig == null) {
			serverConfig = new JsonObject();
			config.add("server", serverConfig);
		}
		serverConfig.addProperty("discoveryIssuer", issuer);
		serverConfig.addProperty("discoveryUrl", issuer + "/.well-known/oauth-authorization-server");

		logSuccess("Set discovery URL from authorization server issuer",
			args("issuer", issuer, "discoveryUrl", issuer + "/.well-known/oauth-authorization-server"));

		return env;
	}
}
