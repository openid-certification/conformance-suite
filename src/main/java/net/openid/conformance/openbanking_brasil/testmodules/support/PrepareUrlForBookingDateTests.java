package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForBookingDateTests extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "base_resource_url")
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("base_resource_url");
		env.putString("protected_resource_url", resourceUrl);
		logSuccess("URL for booking date tests set up: " + resourceUrl);
		return env;
	}
}
