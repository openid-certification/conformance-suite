package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingFinancingContractWarrantiesResource extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"contractId", "base_resource_url"})
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("base_resource_url");
		String contractId = env.getString("contractId");
		resourceUrl = String.format("%s/%s/warranties", resourceUrl, contractId);
		env.putString("protected_resource_url", resourceUrl);
		logSuccess("URL for contract warranties set up");
		return env;
	}
}
