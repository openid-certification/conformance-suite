package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractScopeAddingCondition;

public class AddUnarrangedOverdraftScope extends AbstractScopeAddingCondition {
	@Override
	protected String newScope() {
		return "unarranged-accounts-overdraft";
	}
}
