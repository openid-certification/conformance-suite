package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareAllCustomerBusinessRelatedConsentsForHappyPathTest extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String[] permissions = {"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ",
			"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ", "RESOURCES_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		return env;
	}
}
