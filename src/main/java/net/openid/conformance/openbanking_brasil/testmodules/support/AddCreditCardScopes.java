package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddCreditCardScopes extends AbstractScopeAddingCondition {

	@Override
	protected String newScope() {
		return "credit-cards-accounts";
	}
}
