package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingCreditAdvanceContractInstallments extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		String contractId = env.getString("contractId");

		setApi("unarranged-accounts-overdraft");
		setEndpoint("/contracts/" + contractId + "/scheduled-instalments");

		return super.evaluate(env);

	}
}
