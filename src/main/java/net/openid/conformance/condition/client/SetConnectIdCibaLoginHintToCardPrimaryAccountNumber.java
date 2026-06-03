package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetConnectIdCibaLoginHintToCardPrimaryAccountNumber extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String cardPrimaryAccountNumber = env.getString("config", "client.card_primary_account_number");
		if (Strings.isNullOrEmpty(cardPrimaryAccountNumber)) {
			throw error("'Card primary account number' field is missing from the 'Client' section in the test configuration");
		}

		env.putString("config", "client.hint_type", "login_hint");
		env.putString("config", "client.hint_value", cardPrimaryAccountNumber);

		logSuccess("Set ConnectID CIBA login_hint to a card primary account number",
			args("login_hint", cardPrimaryAccountNumber));

		return env;
	}
}
