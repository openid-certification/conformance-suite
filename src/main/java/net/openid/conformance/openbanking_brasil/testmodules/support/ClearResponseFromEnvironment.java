package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ClearResponseFromEnvironment extends AbstractCondition {

	@Override
	@PreEnvironment(required = "errored_response")
	public Environment evaluate(Environment env) {
		env.removeObject("errored_response");
		logSuccess("Cleared out errored response for future calls");
		return env;
	}
}
