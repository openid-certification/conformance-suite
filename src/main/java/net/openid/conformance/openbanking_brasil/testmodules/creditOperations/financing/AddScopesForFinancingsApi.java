package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing;

import net.openid.conformance.openbanking_brasil.testmodules.support.AbstractScopeAddingCondition;

public class AddScopesForFinancingsApi extends AbstractScopeAddingCondition {

	@Override
	protected String newScope() {
		return "financings";
	}
}
