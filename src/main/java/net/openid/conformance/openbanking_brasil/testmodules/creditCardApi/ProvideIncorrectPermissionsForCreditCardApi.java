package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.condition.AbstractCondition;

public class ProvideIncorrectPermissionsForCreditCardApi extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String[] permissions = {"ACCOUNTS_READ",
			"ACCOUNTS_BALANCES_READ",
			"RESOURCES_READ",
			"ACCOUNTS_TRANSACTIONS_READ",
			"ACCOUNTS_OVERDRAFT_LIMITS_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		return env;
	}
}
