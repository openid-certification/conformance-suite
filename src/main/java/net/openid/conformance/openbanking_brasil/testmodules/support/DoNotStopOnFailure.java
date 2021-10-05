package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class DoNotStopOnFailure extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.mapKey("doNotStopOnFailure", "true");
	    log("Set logging all errors");
		return env;
	}
}
