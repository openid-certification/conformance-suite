package net.openid.conformance.openbanking_brasil.account.v2;

import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingAccountTransactionLimit extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		String accountId = env.getString("accountId");

		setApi("accounts");
		setEndpoint("/accounts/" + accountId + "/overdraft-limits");

		return super.evaluate(env);
	}
}
