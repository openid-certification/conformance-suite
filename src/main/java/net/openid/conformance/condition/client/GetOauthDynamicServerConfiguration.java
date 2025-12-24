package net.openid.conformance.condition.client;

public class GetOauthDynamicServerConfiguration extends GetDynamicServerConfiguration {

	@Override
	protected String getConfigurationEndpoint() {
		return "/.well-known/oauth-authorization-server";
	}
}
