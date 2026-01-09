package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetHintTypeToLoginHint extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.putString("config", "client.hint_type", "login_hint");
		logSuccess("Set hint type to login_hint");
		return env;
	}

}
