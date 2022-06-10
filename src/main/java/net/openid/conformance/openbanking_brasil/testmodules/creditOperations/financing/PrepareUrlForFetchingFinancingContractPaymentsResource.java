package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingFinancingContractPaymentsResource extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		String contractId = env.getString("contractId");

		setApi("financings");
		setEndpoint("/contracts/" + contractId + "/payments");

		return super.evaluate(env);


	}
}
