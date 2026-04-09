package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.OAuthUriUtil;

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

		String discoveryUrl = OAuthUriUtil.generateWellKnownUrlForPath(issuer, "oauth-authorization-server");

		JsonObject config = env.getObject("config");
		JsonObject serverConfig = config.getAsJsonObject("server");
		if (serverConfig == null) {
			serverConfig = new JsonObject();
			config.add("server", serverConfig);
		}
		serverConfig.addProperty("discoveryIssuer", issuer);
		serverConfig.addProperty("discoveryUrl", discoveryUrl);

		logSuccess("Set discovery URL from authorization server issuer",
			args("issuer", issuer, "discoveryUrl", discoveryUrl));

		return env;
	}
}
