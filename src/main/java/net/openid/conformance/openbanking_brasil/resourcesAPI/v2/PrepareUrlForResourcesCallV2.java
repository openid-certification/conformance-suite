package net.openid.conformance.openbanking_brasil.resourcesAPI.v2;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForResourcesCallV2 extends ResourceBuilder {
	@Override
	@PreEnvironment(required = "config")
	@PostEnvironment(strings = "protected_resource_url")
	public Environment evaluate(Environment env) {
		// Takes the consents URL, leaves its base and attaches the resources endpoint
		setAllowDifferentBaseUrl(true);
		setApi("resources");
		setEndpoint("/resources");
		return super.evaluate(env);
	}
}
