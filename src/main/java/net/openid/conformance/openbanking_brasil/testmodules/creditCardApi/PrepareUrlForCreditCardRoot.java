package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForCreditCardRoot extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("base_resource_url");
		env.putString("protected_resource_url", resourceUrl);
		logSuccess("URL set up to call all CreditCard resource");
		return env;
	}
}
