package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments extends ResourceBuilder {

	@Override
	@PreEnvironment(strings = {"contractId", "base_resource_url"})
	public Environment evaluate(Environment env) {

		String contractId = env.getString("contractId");

		setApi("invoice-financings");
		setEndpoint("/contracts/" + contractId + "/scheduled-instalments");

		return super.evaluate(env);
	}
}
