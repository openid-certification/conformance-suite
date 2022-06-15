package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingBillTransactionResource extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		String accountId = env.getString("accountId");
		String billId = env.getString("billId");

		setApi("credit-cards-accounts");
		setEndpoint("/accounts/" + accountId + "/bills/" + billId + "/transactions");

		return super.evaluate(env);
	}

}
