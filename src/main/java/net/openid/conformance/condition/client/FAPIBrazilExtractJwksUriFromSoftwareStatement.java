package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilExtractJwksUriFromSoftwareStatement extends AbstractCondition {

	@Override
	@PreEnvironment(required = "software_statement_assertion")
	@PostEnvironment(strings = "jwks_uri")
	public Environment evaluate(Environment env) {
		// note that software_jwks_uri differs from rfc7591, which would use jwks_uri
		// This is as per the Brazil (and UK) OpenBanking dynamic client registration specification
		String jwksUri = env.getString("software_statement_assertion", "claims.software_jwks_uri");

		if (Strings.isNullOrEmpty(jwksUri)) {
			throw error("Software statement 'software_jwks_uri' missing or empty");
		}

		env.putString("jwks_uri", jwksUri);

		logSuccess("Extracted JWKS URI from software statement",
			args("jwks_uri", jwksUri));

		return env;
	}

}
