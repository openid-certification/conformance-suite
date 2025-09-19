package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFLogSuccessCondition extends AbstractCondition {

	protected final String message;

	public OIDSSFLogSuccessCondition(String message) {
		this.message = message;
	}

	@Override
	public Environment evaluate(Environment env) {

		logSuccess(message);
		return env;
	}
}
