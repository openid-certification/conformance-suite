package net.openid.conformance.openbanking_brasil.testmodules.support;

public class AddResourcesScope extends AbstractScopeAddingCondition{
	@Override
	protected String newScope() {
		return "resources";
	}
}
