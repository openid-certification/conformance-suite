package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddOpenIdScope extends AbstractScopeAddingCondition {

	@Override
	protected String newScope() {
		return "openid";
	}
}
