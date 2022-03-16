package net.openid.conformance.logging;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SimpleLoggingCondition extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		return env;
	}
}
