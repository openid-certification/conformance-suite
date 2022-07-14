package net.openid.conformance.openbanking_brasil.testmodules.account;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openbanking_brasil.testmodules.support.resource.ResourceBuilder;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;

public class PrepareUrlForFetchingCurrentAccountTransactions extends ResourceBuilder {


	@Override
	@PreEnvironment(strings = {"accountId", "protected_resource_url"})
	public Environment evaluate(Environment env) {

		String accountId = env.getString("accountId");

		setApi("accounts");
		setEndpoint("/accounts/" + accountId + "/transactions-current");

		return super.evaluate(env);
	}
}
