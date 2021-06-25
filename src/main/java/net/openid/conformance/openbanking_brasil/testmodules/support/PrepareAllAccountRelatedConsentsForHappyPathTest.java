package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareAllAccountRelatedConsentsForHappyPathTest extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String[] permissions = {"ACCOUNTS_READ",
								"ACCOUNTS_BALANCES_READ",
								"ACCOUNTS_TRANSACTIONS_READ",
								"ACCOUNTS_OVERDRAFT_LIMITS_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		return env;
	}

}
