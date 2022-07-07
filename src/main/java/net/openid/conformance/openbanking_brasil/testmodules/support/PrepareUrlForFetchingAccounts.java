package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingAccounts extends ResourceBuilder {
	@Override
	public Environment evaluate(Environment env) {

		setApi("accounts");
		setEndpoint("/accounts/");

		return super.evaluate(env);
	}
}
