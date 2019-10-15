package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class EnsureResourceAssertionTypeIsJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = "resource_assertion")
	public Environment evaluate(Environment env) {

		String assertionType = env.getString("resource_assertion", "assertion_type");

		String expected = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

		if (expected.equals(assertionType)) {
			logSuccess("Found JWT assertion type");
			return env;
		} else {
			throw error("Assertion type was not JWT", args("expected", expected, "actual", assertionType));
		}

	}

}
