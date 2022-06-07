package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingAccountTransactions extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		String accountId = env.getString("accountId");

		setApi("accounts");
		setEndpoint("/accounts/" + accountId + "/transactions");

		return super.evaluate(env);
	}
}
