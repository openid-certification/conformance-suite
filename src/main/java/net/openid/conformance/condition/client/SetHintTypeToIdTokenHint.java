package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class SetHintTypeToIdTokenHint extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		env.putString("config", "client.hint_type", "id_token_hint");
		logSuccess("Set hint type to id_token_hint");
		return env;
	}

}
