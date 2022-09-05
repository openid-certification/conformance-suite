package net.openid.conformance.condition.as.logout;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class LogoutByRemovingSessionState extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"session_state_data"})
	@PostEnvironment()
	public Environment evaluate(Environment env) {
		env.removeObject("session_state_data");

		log("Removed session state");

		return env;

	}

}
