package net.openid.conformance.openbanking_brasil.plans;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareAllCreditCardRelatedConsentsForHappyPathTest extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {

		String[] permissions = {"CREDIT_CARDS_ACCOUNTS_READ",
			"CREDIT_CARDS_ACCOUNTS_BILLS_READ",
			"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ",
			"CREDIT_CARDS_ACCOUNTS_LIMITS_READ",
		"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		return env;
	}

}
