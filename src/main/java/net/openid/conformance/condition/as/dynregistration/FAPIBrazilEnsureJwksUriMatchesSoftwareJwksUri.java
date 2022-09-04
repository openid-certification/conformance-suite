package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilEnsureJwksUriMatchesSoftwareJwksUri extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "dynamic_registration_request", "software_statement"})
	public Environment evaluate(Environment env) {

		String jwksUri = env.getString("dynamic_registration_request", "jwks_uri");
		String ssJwksUri = env.getString("software_statement", "claims.software_jwks_uri");
		if(jwksUri==null) {
			throw error("Registration request does not contain a jwks_uri");
		}
		if(jwksUri.equals(ssJwksUri)) {
			logSuccess("jwks_uri matches software_jwks_uri");
			return env;
		} else {
			throw error("jwks_uri does not match software_jwks_uri", args("jwks_uri", jwksUri, "software_jwks_uri", ssJwksUri));
		}

	}
}
