package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareAllCreditAdvancesRelatedConsentsForHappyPathTest extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String[] permissions = {"UNARRANGED_ACCOUNTS_OVERDRAFT_READ",
			"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ",
			"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ",
			"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		return env;
	}

}
