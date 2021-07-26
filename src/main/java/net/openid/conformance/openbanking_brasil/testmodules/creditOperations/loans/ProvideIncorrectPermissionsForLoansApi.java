package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ProvideIncorrectPermissionsForLoansApi extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String[] permissions = {"CREDIT_CARDS_ACCOUNTS_READ", "CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ", "RESOURCES_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		log("Requesting permissions which should not be usable for financing resources");
		return env;
	}
}
