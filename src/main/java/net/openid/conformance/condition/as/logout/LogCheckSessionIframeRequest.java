package net.openid.conformance.condition.as.logout;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class LogCheckSessionIframeRequest extends AbstractCondition {


	@Override
	public Environment evaluate(Environment env) {
		log("The client requested check_session_iframe");
		return env;
	}
}
