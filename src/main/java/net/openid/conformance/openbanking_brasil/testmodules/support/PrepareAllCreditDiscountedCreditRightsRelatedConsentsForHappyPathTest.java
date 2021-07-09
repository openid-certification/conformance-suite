package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareAllCreditDiscountedCreditRightsRelatedConsentsForHappyPathTest extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "consent_permissions")
	public Environment evaluate(Environment env) {

		String[] permissions = {"INVOICE_FINANCINGS_READ",
			"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ",
			"INVOICE_FINANCINGS_PAYMENTS_READ",
			"INVOICE_FINANCINGS_WARRANTIES_READ"};
		env.putString("consent_permissions", String.join(" ", permissions));
		return env;
	}

}
