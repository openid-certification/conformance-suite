package net.openid.conformance.openbanking_brasil.testmodules.account;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingPatrimonialClaim extends ResourceBuilder {

	@Override
	@PreEnvironment(strings = "policyId")
	public Environment evaluate(Environment env) {

		String policyId = env.getString("policyId");
		setApi("insurance-patrimonial");
		setEndpoint(String.format("/%s/claim", policyId));

		return super.evaluate(env);
	}
}
