package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class VCICheckExpClaimInCredential extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "sdjwt" } )
	public Environment evaluate(Environment env) {

		Instant now = Instant.now(); // to check timestamps

		Long exp = env.getLong("sdjwt", "credential.claims.exp");
		if (exp == null) {
			throw error("'exp' claim missing");
		}
		if (now.isAfter(Instant.ofEpochSecond(exp))) {
			throw error("Credential 'exp' has expired", args("expires-at", new Date(exp * 1000L), "now", now));
		}

		logSuccess("Credential 'exp' passed validation checks");
		return env;

	}
}
