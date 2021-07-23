package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetUpCustomerPersonalIdOnlyPermissions extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String[] permissions = {"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		logSuccess("Set incomplete permissions for personal identification", args("permissions", permissions));
		return env;
	}
}
