package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.Date;

public class FAPIBrazilValidateSoftwareStatementIat extends AbstractCondition {
	//shall validate that the software_statement was issued (iat) not more than 5 minutes prior to the request being received
	private static final int maxDuration = 5 * 60 * 1000;
	@Override
	@PreEnvironment(required = { "software_statement"})
	public Environment evaluate(Environment env) {
		Long iat = env.getLong("software_statement", "claims.iat");

		if (iat == null) {
			throw error("Missing issuance time, iat, claim");
		}
		Instant now = Instant.now();
		if (now.minusMillis(maxDuration).isAfter(Instant.ofEpochSecond(iat))) {
			throw error("Token issued mote than 5 minutes ago", args("issued-at", new Date(iat * 1000L), "now", now));
		}
		logSuccess("Software statement was issued (iat) not more than 5 minutes ago", args("issued-at", new Date(iat * 1000L), "now", now));
		return env;
	}
}
