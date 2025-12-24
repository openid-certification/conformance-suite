package net.openid.conformance.condition.client;

import java.net.URL;

public class CheckOauthDiscEndpointDiscoveryUrl extends CheckDiscEndpointDiscoveryUrl {

	@Override
	protected String getConfigurationEndpoint() {
		return "/.well-known/oauth-authorization-server";
	}

	@Override
	protected boolean isValidDiscoveryUrl(URL url) {
		// check /.well-known/oauth-authorization-server and then fallback to super's /.well-known/openid-configuration
		return url.getPath().startsWith(getConfigurationEndpoint()) || url.getPath().endsWith(super.getConfigurationEndpoint());
	}
}
