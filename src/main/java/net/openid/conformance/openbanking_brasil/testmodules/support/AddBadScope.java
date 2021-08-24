package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddBadScope extends AbstractScopeAddingCondition {
	@Override
	protected String newScope() {
		return "bad-scope";
	}
}
