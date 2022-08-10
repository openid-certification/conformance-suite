package net.openid.conformance.openinsurance.testmodule.support;

import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractScopeAddingCondition;

public class AddScopesForCustomerApi extends AbstractScopeAddingCondition {

	@Override
	protected String newScope() {
		return "customers";
	}

}
