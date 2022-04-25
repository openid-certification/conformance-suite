package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ProvideIncorrectPermissionsForAccountsApi extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {
		String productType = env.getString("config", "consent.productType");
		String[] permissions;

		if (productType.equals("business")) {
			permissions = new String[]{"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ", "CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ" , "RESOURCES_READ"};
		} else {
			permissions = new String[]{"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ", "CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ", "RESOURCES_READ"};	
		}
		env.putString("consent_permissions", String.join(" ", permissions));
		log("Requesting permissions which should not be usable for accounts resources");
		return env;
	}

}
