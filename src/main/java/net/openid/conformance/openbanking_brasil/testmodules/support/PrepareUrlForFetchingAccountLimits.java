package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingAccountLimits extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"accountId", "base_resource_url"})
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("base_resource_url");
		String accountId = env.getString("accountId");
		resourceUrl = String.format("%s/%s/overdraft-limits", resourceUrl, accountId);
		env.putString("protected_resource_url", resourceUrl);
		logSuccess("URL for account limits set up");
		return env;
	}
}