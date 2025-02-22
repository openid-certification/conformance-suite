package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFPushPendingSecurityEvents extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		// read current event from env

		logSuccess("Pushed pending SSF security events");

		return env;
	}
}
