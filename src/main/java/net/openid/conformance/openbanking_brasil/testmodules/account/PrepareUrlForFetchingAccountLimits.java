package net.openid.conformance.openbanking_brasil.testmodules.account;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

public class PrepareUrlForFetchingAccountLimits extends ResourceBuilder {

	@Override
	public Environment evaluate(Environment env) {

		String accountId = env.getString("accountId");

		setApi("accounts");
		setEndpoint("/accounts/" + accountId + "/overdraft-limits");

		return super.evaluate(env);
	}
}