package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddAccountScope extends AbstractScopeAddingCondition {
	@Override
	protected String newScope() {
		return "accounts";
	}
}
