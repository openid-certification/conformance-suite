package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingCreditCardAccount extends ResourceBuilder {

	@Override
	@PreEnvironment(strings = "accountId")
	public Environment evaluate(Environment env) {

		String accountId = env.getString("accountId");

		setApi("credit-cards-accounts");
		setEndpoint("/accounts/" + accountId);

		return super.evaluate(env);
	}
}
