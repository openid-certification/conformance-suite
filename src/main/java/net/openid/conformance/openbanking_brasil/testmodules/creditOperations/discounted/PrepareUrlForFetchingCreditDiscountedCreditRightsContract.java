package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingCreditDiscountedCreditRightsContract extends ResourceBuilder {

	@Override
	@PreEnvironment(strings = {"contractId"})
	public Environment evaluate(Environment env) {

		String contractId = env.getString("contractId");

		setApi("invoice-financings");
		setEndpoint("/contracts/" + contractId);

		return super.evaluate(env);
	}
}
