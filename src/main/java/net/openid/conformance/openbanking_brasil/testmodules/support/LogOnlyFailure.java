package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class LogOnlyFailure extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.mapKey("logOnlyFailure", "true");
	    log("Setting logging only errors is ON");
		return env;
	}
}
