package io.fintechlabs.testframework.condition.client;

public class CopyScopeFromDynamicRegistrationTemplateToClientConfiguration extends AbstractCopyConfigFromDynamicRegistrationTemplateToClientConfiguration {

	@Override
	protected String getExpectedConfigName() {
		return "scope";
	}
}
