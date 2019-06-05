package io.fintechlabs.testframework.condition.rs;

import org.apache.commons.lang3.RandomStringUtils;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class GenerateOpenBankingAccountId extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "account_id")
	public Environment evaluate(Environment env) {

		String acct = RandomStringUtils.randomAlphanumeric(10);

		env.putString("account_id", acct);

		logSuccess("Created account", args("account", acct));

		return env;

	}

}
