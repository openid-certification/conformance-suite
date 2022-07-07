package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForRequestingFirstAccountResource extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		String accountId = env.getString("accountId_1");

		setApi("accounts");
		setEndpoint("/accounts/" + accountId);

		return super.evaluate(env);
	}
}
