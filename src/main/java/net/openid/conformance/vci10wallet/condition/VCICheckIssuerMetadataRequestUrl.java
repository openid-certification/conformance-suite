package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;

public class VCICheckIssuerMetadataRequestUrl extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String requestUrl = env.getString("incoming_request", "request_url");

		String serverIssuer = env.getString("server", "issuer");
		URI serverIssuerUri = URI.create(serverIssuer);
		String serverIssuerPath = serverIssuerUri.getPath();

		String expectedPath = "/.well-known/openid-credential-issuer" + serverIssuerPath;
		URI requestUri = URI.create(requestUrl);

		if (!expectedPath.equals(requestUri.getPath())) {
			throw error("Issuer metadata request does not match expected URL path", args("expected_path", expectedPath, "request_url", requestUrl));
		}

		logSuccess("Issuer metadata request is for correct URL", args("request_url", requestUrl));
		return env;
	}
}
