package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddPatrimonialScope extends AbstractScopeAddingCondition {
	@Override
	protected String newScope() {
		return "insurance-patrimonial";
	}
}
