package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingCreditAdvanceRoot extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		String contractId = env.getString("contractId");

		setApi("unarranged-accounts-overdraft");
		setEndpoint("/contracts");

		return super.evaluate(env);
	}
}
