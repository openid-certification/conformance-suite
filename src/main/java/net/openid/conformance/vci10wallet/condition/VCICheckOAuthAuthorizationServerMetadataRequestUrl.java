package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.OAuthUriUtil;

import java.net.URI;

public class VCICheckOAuthAuthorizationServerMetadataRequestUrl extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String requestUrl = env.getString("incoming_request", "request_url");

		String serverIssuer = env.getString("server", "issuer");
		URI serverIssuerUri = URI.create(serverIssuer);
		String serverIssuerPath = serverIssuerUri.getPath();
		serverIssuerPath = OAuthUriUtil.stripTrailingSlash(serverIssuerPath);

		String expectedPath = "/.well-known/oauth-authorization-server" + serverIssuerPath;
		URI requestUri = URI.create(requestUrl);

		// RFC 8414 section 3.1 requires the terminating "/" of the issuer to be removed
		// before inserting "/.well-known/...". Many wallets keep it; rather than failing
		// either form, accept both by stripping a single trailing "/" before comparing.
		if (!OAuthUriUtil.stripTrailingSlash(expectedPath).equals(requestUri.getPath())) {
			throw error("Auth Server metadata request does not match expected URL path", args("expected_path", expectedPath, "request_url", requestUrl));
		}

		logSuccess("OAuth authorization server metadata request is for correct URL", args("request_url", requestUrl));
		return env;
	}
}
