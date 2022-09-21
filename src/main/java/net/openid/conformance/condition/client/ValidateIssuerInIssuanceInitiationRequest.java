package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateIssuerInIssuanceInitiationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "issuance_initiation_request", "server" } )
	public Environment evaluate(Environment env) {

		String issuer = env.getString("server", "issuer");

		String authResponseIssuer = env.getString("issuance_initiation_request", "issuer");

		if (authResponseIssuer == null) {
			log("No 'issuer' value in issuance initiation request.");
			return env;
		}

		if (!issuer.equals(authResponseIssuer)) {
			throw error("'issuer' parameter in issuance initiation request does not match server's issuer value.",
				args("expected", issuer, "actual", authResponseIssuer));
		}

		logSuccess("'iss' parameter in issuance initiation request matches server's issuer value.");

		return env;

	}

}
