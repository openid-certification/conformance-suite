package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi;

import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractScopeAddingCondition;

public class AddScopesForCreditCardApi extends AbstractScopeAddingCondition {

	@Override
	protected String newScope() {
		return "credit-cards-accounts";
	}

}
