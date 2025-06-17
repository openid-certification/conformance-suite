package net.openid.conformance.vciid2wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;

public class VCICheckOAuthAuthorizationServerMetadataRequest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String method = env.getString("incoming_request", "method");
		if (!"GET".equals(method)) {
			throw error("OAuth authorization server metadata request does not support method '" + method + "'");
		}

		String requestUrl = env.getString("incoming_request", "request_url");

		String serverIssuer = env.getString("server", "issuer");
		URI serverIssuerUri = URI.create(serverIssuer);
		String serverIssuerPath = serverIssuerUri.getPath();

		String expectedRequestUrl = serverIssuerUri.getScheme() + "://" + serverIssuerUri.getAuthority() + "/.well-known/oauth-authorization-server" + serverIssuerPath;

		if (!expectedRequestUrl.equals(requestUrl)) {
			throw error("Auth Server metadata request does not match expected URL", args("expected_url", expectedRequestUrl, "request_url", requestUrl));
		}

		logSuccess("Detected expected OAuth authorization server metadata request", args("request_url", requestUrl));
		return env;
	}
}
