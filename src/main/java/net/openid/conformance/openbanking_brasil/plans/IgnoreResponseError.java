package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class IgnoreResponseError extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.putBoolean("ignore_response_errors", true);
		return env;
	}
}
