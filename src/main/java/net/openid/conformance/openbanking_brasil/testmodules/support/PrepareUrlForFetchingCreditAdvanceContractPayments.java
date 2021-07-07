package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingCreditAdvanceContractPayments  extends AbstractCondition {

	@Override
	@PreEnvironment(strings = {"contractId", "base_resource_url"})
	public Environment evaluate(Environment env) {
		String resourceUrl = env.getString("base_resource_url");
		String contractId = env.getString("contractId");
		resourceUrl = String.format("%s/%s/payments", resourceUrl, contractId);
		env.putString("protected_resource_url", resourceUrl);
		return env;
	}
}
