package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckDiscEndpointIssuer extends AbstractCondition {

	@Override
	@PostEnvironment(required = { "server", "config" } )
	public Environment evaluate(Environment env) {
		JsonElement issuerElement = env.getElementFromObject("server", "issuer");

		if (issuerElement == null || issuerElement.isJsonObject()) {

			throw error("issuer is missing from discovery endpoint document");
		}

		String discoveryUrl = env.getString("config", "server.discoveryUrl");

		String issuerUrl = OIDFJSON.getString(issuerElement);

		final String removingPartInUrl = ".well-known/openid-configuration";
		if (discoveryUrl.endsWith(removingPartInUrl)) {

			discoveryUrl = discoveryUrl.substring(0, discoveryUrl.length() - removingPartInUrl.length());
		}

		//Remove slash character endpoint url before comparing
		if (!removeSlashEndpointURL(issuerUrl).equals(removeSlashEndpointURL(discoveryUrl))) {

			throw error("issuer listed in the discovery document is not consistent with the location the discovery document was retrieved from. These must match to prevent impersonation attacks.", args("discovery_url", discoveryUrl, "issuer", issuerUrl));
		}

		logSuccess("issuer is consistent with the discovery endpoint");

		return env;
	}

	private String removeSlashEndpointURL(String url) {
		if (url.endsWith("/")) {

			return url.substring(0, url.length() - 1);
		}

		return url;
	}
}
