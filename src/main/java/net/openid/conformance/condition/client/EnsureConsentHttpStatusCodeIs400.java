package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class EnsureConsentHttpStatusCodeIs400 extends AbstractCondition {
	@Override
	public Environment evaluate(Environment env) {
		env.putObject(
			"endpoint_response",
			env.getObject("consent_endpoint_response_full")
		);
		return env;
	}
}
