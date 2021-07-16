package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ProvideIncorrectPermissionsForAccountsApi extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String[] permissions = {"CREDIT_CARDS_ACCOUNTS_READ", "CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ", "RESOURCES_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		log("Requesting permissions which should not be usable for accounts resources");
		return env;
	}

}
