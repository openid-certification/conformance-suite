package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AccountsOperationLimitsSelectResourceOne extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "accountId_1")
	@PostEnvironment(strings = "accountId")
	public Environment evaluate(Environment env) {
		String accountIdOne = env.getString("accountId_1");
		env.putString("accountId",accountIdOne);
		return env;
	}
}
