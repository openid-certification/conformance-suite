package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetConnectIdCibaLoginHintToCardPrimaryAccountNumber extends AbstractCondition {

	public static final String CARD_PRIMARY_ACCOUNT_NUMBER = "6372069742108725";

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		env.putString("config", "client.hint_type", "login_hint");
		env.putString("config", "client.hint_value", CARD_PRIMARY_ACCOUNT_NUMBER);

		logSuccess("Set ConnectID CIBA login_hint to a card primary account number",
			args("login_hint", CARD_PRIMARY_ACCOUNT_NUMBER));

		return env;
	}
}
