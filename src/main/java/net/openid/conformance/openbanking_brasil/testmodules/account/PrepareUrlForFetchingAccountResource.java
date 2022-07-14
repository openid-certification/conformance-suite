package net.openid.conformance.openbanking_brasil.testmodules.account;

import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingAccountResource extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		String accountId = env.getString("accountId");
		setApi("accounts");
		setEndpoint("/accounts/" + accountId);

		return super.evaluate(env);
	}
}
