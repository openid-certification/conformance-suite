package net.openid.conformance.openbanking_brasil.testmodules.support;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AccountOperationalLimitsSelectResourceTwo extends AbstractCondition {
	@Override
	@PreEnvironment(strings = "accountId_2")
	@PostEnvironment(strings = "accountId")
	public Environment evaluate(Environment env) {
		String accountIdTwo = env.getString("accountId_2");
		env.putString("accountId",accountIdTwo);
		return env;	}
}
