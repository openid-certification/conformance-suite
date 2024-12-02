package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class DeriveOauthProtectedResourceMetadataUri extends AbstractCondition {

	@PreEnvironment(required = "server")
	@Override
	public Environment evaluate(Environment env) {

		String issuer = env.getString("server", "issuer");

		if (issuer == null) {
			logFailure("Could not find issuer to derive oauth_protected_resource_metadata_uri.");
			return env;
		}

		String oauthProtectedResourceUri = issuer;
		if (!issuer.endsWith("/")) {
			oauthProtectedResourceUri += "/";
		}
		oauthProtectedResourceUri += ".well-known/oauth-protected-resource";

		env.putString("server", "oauth_protected_resource_metadata_uri", oauthProtectedResourceUri);

		logSuccess("Derived oauth_protected_resource_metadata_uri from issuer", args("issuer", issuer, "oauth_protected_resource_metadata_uri", oauthProtectedResourceUri));

		return env;
	}
}
