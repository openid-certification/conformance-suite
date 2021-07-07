package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RequestFinancingsReadAndPaymentsReadOnly extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String[] permissions = {"FINANCINGS_READ", "FINANCINGS_PAYMENTS_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		return env;
	}

}
