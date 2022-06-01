package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class PrepareUrlForFetchingCurrentAccountTransactions extends AbstractCondition {


	@Override
	@PreEnvironment(strings = {"accountId", "protected_resource_url"})
	@PostEnvironment(strings = "base_resource_url")
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("protected_resource_url");
		env.putString("base_resource_url", resourceUrl);
		String accountId = env.getString("accountId");
		resourceUrl = String.format("%s/%s/transactions-current", resourceUrl, accountId);
		env.putString("protected_resource_url", resourceUrl);
		logSuccess("URL for account transactions-current is set up", Map.of("URL", resourceUrl));
		return env;
	}
}
