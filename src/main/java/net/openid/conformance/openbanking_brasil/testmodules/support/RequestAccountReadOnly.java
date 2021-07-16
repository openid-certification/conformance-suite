package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RequestAccountReadOnly extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String[] permissions = {"ACCOUNTS_READ", "ACCOUNTS_TRANSACTIONS_READ", "RESOURCES_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		logSuccess("Requested consent will be for the ACCOUNTS_READ, ACCOUNTS_TRANSACTIONS_READ and RESOURCES_READ permissions only");
		return env;
	}

}
