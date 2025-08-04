package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;

public class VCICheckOAuthAuthorizationServerMetadataRequestUrl extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String requestUrl = env.getString("incoming_request", "request_url");

		String serverIssuer = env.getString("server", "issuer");
		URI serverIssuerUri = URI.create(serverIssuer);
		String serverIssuerPath = serverIssuerUri.getPath();

		String expectedRequestUrl = serverIssuerUri.getScheme() + "://" + serverIssuerUri.getAuthority() + "/.well-known/oauth-authorization-server" + serverIssuerPath;

		if (!expectedRequestUrl.equals(requestUrl)) {
			throw error("Auth Server metadata request does not match expected URL", args("expected_url", expectedRequestUrl, "request_url", requestUrl));
		}

		logSuccess("OAuth authorization server metadata request is for correct URL", args("request_url", requestUrl));
		return env;
	}
}
