package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddPaymentScope extends AbstractScopeAddingCondition {
	@Override
	protected String newScope() {
		return "payments";
	}
}
