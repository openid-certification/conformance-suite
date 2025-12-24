package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;

import java.net.MalformedURLException;
import java.net.URL;

public class CheckOauthDiscEndpointIssuer extends CheckDiscEndpointIssuer {

	@Override
	protected String getConfigurationEndpoint() {
		return "/.well-known/oauth-authorization-server";
	}

	@Override
	protected String getExpectedIssuerUrl(Environment env) {
		// OAuth discovery path must start with .well-known/oauth-authorization-server e.g. https://www.example.com/.well-known/oauth-authorization-server/issuerPath
		String discoveryUrl = getConfigurationUrl(env);
		try{
			if(discoveryUrl.contains(getConfigurationEndpoint())) {  // OAuth discovery URL
				URL url = new URL(discoveryUrl);
				if(url.getPath().startsWith(getConfigurationEndpoint())) {
					final String removingPartInUrl = getConfigurationEndpoint();
					int foundIndex = discoveryUrl.indexOf(removingPartInUrl);
					if(foundIndex != -1) {
						discoveryUrl = discoveryUrl.substring(0, foundIndex) + discoveryUrl.substring(foundIndex + getConfigurationEndpoint().length());
					}
					return discoveryUrl;
				} else {
					throw error("Invalid OAuth discovery URl", args("url", discoveryUrl));
				}
			} else { // fallback on openid-configuration
				final String removingPartInUrl = super.getConfigurationEndpoint();
				if (discoveryUrl.endsWith(removingPartInUrl)) {
					discoveryUrl = discoveryUrl.substring(0, discoveryUrl.length() - removingPartInUrl.length());
				}
				return discoveryUrl;
			}
		} catch (MalformedURLException e) {
			throw error("Malformed discovery URL", e, args("url", discoveryUrl));
		}
	}
}
