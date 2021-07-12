package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingLoanContractResource extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"contractId"})
	@PostEnvironment(strings = "base_resource_url")
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("protected_resource_url");
		env.putString("base_resource_url", resourceUrl);
		String contractId = env.getString("contractId");
		resourceUrl = String.format("%s/%s", resourceUrl, contractId);
		env.putString("protected_resource_url", resourceUrl);
		return env;
	}
}
