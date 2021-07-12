package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddConsentScope extends AbstractScopeAddingCondition {
	@Override
	protected String newScope() {
		return "consents";
	}
}
