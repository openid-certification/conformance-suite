package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckDiscEndpointIssuer extends AbstractCondition {

	@Override
	@PostEnvironment(required = { "server", "config" } )
	public Environment evaluate(Environment env) {

		String endpointLabel = getEndpointLabel();

		JsonElement issuerElement = getResponseIssuerElement(env);

		if (issuerElement == null || issuerElement.isJsonObject()) {

			throw error("issuer is missing from " + endpointLabel + " endpoint document");
		}

		String issuerUrl = OIDFJSON.getString(issuerElement);

		String discoveryUrl = getExpectedIssuerUrl(env);

		//Remove slash character endpoint url before comparing
		if (!removeSlashEndpointURL(issuerUrl).equals(removeSlashEndpointURL(discoveryUrl))) {

			throw createIssuerMismatchError(env, issuerUrl, discoveryUrl);
		}

		logSuccess("issuer is consistent with the " + endpointLabel + " endpoint", args("issuer", issuerUrl));

		return env;
	}

	protected String getExpectedIssuerUrl(Environment env) {
		String discoveryUrl = getConfigurationUrl(env);

		final String removingPartInUrl = getConfigurationEndpoint();
		if (discoveryUrl.endsWith(removingPartInUrl)) {

			discoveryUrl = discoveryUrl.substring(0, discoveryUrl.length() - removingPartInUrl.length());
		}
		return discoveryUrl;
	}

	protected ConditionError createIssuerMismatchError(Environment env, String issuerUrl, String discoveryUrl) {
		return error("issuer listed in the discovery document is not consistent with the location the discovery document was retrieved from. These must match to prevent impersonation attacks.", args("discovery_url", discoveryUrl, "issuer", issuerUrl));
	}

	protected String getEndpointLabel() {
		return "discovery";
	}

	protected String getConfigurationUrl(Environment env) {
		return env.getString("config", "server.discoveryUrl");
	}

	protected JsonElement getResponseIssuerElement(Environment env) {
		return env.getElementFromObject("server", "issuer");
	}

	protected String getConfigurationEndpoint() {
		return ".well-known/openid-configuration";
	}

	protected String removeSlashEndpointURL(String url) {
		if (url.endsWith("/")) {

			return url.substring(0, url.length() - 1);
		}

		return url;
	}
}
