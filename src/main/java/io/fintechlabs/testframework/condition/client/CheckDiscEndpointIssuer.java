package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonElement;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class CheckDiscEndpointIssuer extends AbstractCondition {

	private final String removingPartInUrl = ".well-known/openid-configuration";

	public CheckDiscEndpointIssuer(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	@Override
	@PostEnvironment(required = { "server", "config" } )
	public Environment evaluate(Environment env) {
		JsonElement issuerElement = env.getElementFromObject("server", "issuer");

		if (issuerElement == null || issuerElement.isJsonObject()) {

			throw error("issuer in server was missing");
		}

		String discoveryUrl = env.getString("config", "server.discoveryUrl");

		String issuerUrl = issuerElement.getAsString();

		if (discoveryUrl.endsWith(removingPartInUrl)) {

			discoveryUrl = discoveryUrl.substring(0, discoveryUrl.length() - removingPartInUrl.length());
		}

		//Remove slash character endpoint url before comparing
		if (!removeSlashEndpointURL(issuerUrl).equals(removeSlashEndpointURL(discoveryUrl))) {

			throw error("issuer in server did not match the discovery endpoint", args("discovery_url", discoveryUrl, "issuer", issuerUrl));
		}

		logSuccess("issuer matched the discovery endpoint");

		return env;
	}

	private String removeSlashEndpointURL(String url) {
		if (url.endsWith("/")) {

			return url.substring(0, url.length() - 1);
		}

		return url;
	}
}
