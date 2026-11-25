package net.openid.conformance.condition.as.dynregistration;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class ValidateSoftwareStatementExp extends AbstractCondition {

	@Override
	@PreEnvironment(required = "software_statement")
	public Environment evaluate(Environment env) {
		Long exp = env.getLong("software_statement", "claims.exp");
		if (exp == null) {
			throw error("The software statement does not contain an expiry time (exp) claim",
				args("claims", env.getElementFromObject("software_statement", "claims")));
		}
		Instant expiry = Instant.ofEpochSecond(exp);
		Instant now = Instant.now();
		if (!now.isBefore(expiry)) {
			throw error("The software statement has expired", args("exp", expiry, "now", now));
		}
		logSuccess("The software statement has not expired", args("exp", expiry, "now", now));
		return env;
	}
}
