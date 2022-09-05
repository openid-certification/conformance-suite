package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URL;

//PAR-5.0 : If the authorization server has a pushed authorization request endpoint,
//it SHOULD include the following OAuth/OpenID Provider Metadata parameter in discovery responses:
//pushed_authorization_request_endpoint : The URL of the pushed authorization request endpoint at which
//the client can post an authorization request and get a request URI in exchange.
public class CheckDiscEndpointPARSupported extends AbstractCondition {

	@Override
	@PostEnvironment(required = {"server", "config"})
	public Environment evaluate(Environment env) {

		JsonElement parEndpoint = env.getElementFromObject("server", "pushed_authorization_request_endpoint");

		if (parEndpoint == null || parEndpoint.isJsonObject()) {
			throw error("pushed_authorization_request_endpoint is missing from discovery endpoint document");
		}

		String parEndpointUrl = OIDFJSON.getString(parEndpoint);

		//verify parEndpointUrl is a valid https URL
		verifyValidHttpsUrl(parEndpointUrl);

		logSuccess("pushed_authorization_request_endpoint defines a valid https URL");

		return env;
	}

	private void verifyValidHttpsUrl(String parEndpointUrl) {
		URL url = null;
		try {
			url = new URL(parEndpointUrl);
			if (!"https".equalsIgnoreCase(url.getProtocol())) {
				throw error("pushed_authorization_request_endpoint URL does not use https protocol",
					args ("pushed_authorization_request_endpoint", parEndpointUrl));
			}
			//try to convert to URI to make sure its valid
			url.toURI();
		} catch (Exception e) {
			throw error("pushed_authorization_request_endpoint URL is not a valid URL",
				args ("pushed_authorization_request_endpoint", parEndpointUrl));
		}
	}
}
