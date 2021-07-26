package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingCardTransactions extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"accountId", "base_resource_url"})
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("base_resource_url");
		String accountId = env.getString("accountId");
		resourceUrl = String.format("%s/%s/transactions", resourceUrl, accountId);
		env.putString("protected_resource_url", resourceUrl);
		return env;
	}
}
