package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForCreditCardRoot extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		setApi("credit-cards-accounts");
		setEndpoint("/accounts");

		return super.evaluate(env);
	}
}
