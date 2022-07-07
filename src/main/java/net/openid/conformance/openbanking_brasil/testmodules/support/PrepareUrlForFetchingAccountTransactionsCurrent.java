package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingAccountTransactionsCurrent extends ResourceBuilder {
	@Override
	public Environment evaluate(Environment env) {

		String accountId = env.getString("accountId");

		setApi("accounts");
		setEndpoint("/accounts/" + accountId + "/transactions-current");

		return super.evaluate(env);
	}
}
