package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddLoansScope extends AbstractScopeAddingCondition {
	@Override
	protected String newScope() {
		return "loans";
	}
}
