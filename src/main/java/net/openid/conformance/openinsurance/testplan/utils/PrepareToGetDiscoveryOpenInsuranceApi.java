package net.openid.conformance.openinsurance.testplan.utils;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareToGetDiscoveryOpenInsuranceApi extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("config", "resource.resourceUrl");
		String suffix = getRequirements().iterator().next();
		resourceUrl = String.format("%s/%s", resourceUrl, suffix);
		env.putString("protected_resource_url", resourceUrl);
		return env;
	}
}
