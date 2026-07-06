package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class CreateTransactionId extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "transaction_id")
	public Environment evaluate(Environment env) {
		String transactionId = UUID.randomUUID().toString();
		logSuccess("Created new transaction_id ID", args("transaction_id", transactionId));
		env.putString("transaction_id", transactionId);
		return env;
	}

}
