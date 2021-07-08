package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddScopesForFinancingsApi extends AbstractScopeAddingCondition {

	@Override
	protected String newScope() {
		return "financings";
	}
}
