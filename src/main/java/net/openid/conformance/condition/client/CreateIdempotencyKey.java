package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class CreateIdempotencyKey extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "idempotency_key")
	public Environment evaluate(Environment env) {
		String idempotencyKey = UUID.randomUUID().toString();

		env.putString("idempotency_key", idempotencyKey);

		logSuccess("Created idempotency key", args("idempotency_key", idempotencyKey));

		return env;

	}
}
