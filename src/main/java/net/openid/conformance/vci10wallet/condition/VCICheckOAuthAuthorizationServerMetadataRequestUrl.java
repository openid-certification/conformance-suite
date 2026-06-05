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
		// RFC 8414 section 3.1 requires the terminating "/" of the issuer to be removed
		// before inserting "/.well-known/oauth-authorization-server". The suite always
		// configures the issuer with a trailing slash, so strip it here to form the
		// spec-compliant expected path. The incoming request must match it exactly; a
		// request that retains the trailing "/" is non-compliant and is rejected.
		serverIssuerPath = OAuthUriUtil.stripTrailingSlash(serverIssuerPath);

		String expectedPath = "/.well-known/oauth-authorization-server" + serverIssuerPath;
		URI requestUri = URI.create(requestUrl);

		if (!expectedPath.equals(requestUri.getPath())) {
			throw error("Auth Server metadata request does not match expected URL path", args("expected_path", expectedPath, "request_url", requestUrl));
		}

		logSuccess("OAuth authorization server metadata request is for correct URL", args("request_url", requestUrl));
		return env;
	}
}
