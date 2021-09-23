package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Failing seemed easier than implementing full support for idempotency keys
 */
public class FailIfXIdempotencyKeyHeaderAlreadyExists extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String header = env.getString("idempotency_key");
		if (!Strings.isNullOrEmpty(header)) {
			throw error("x-idempotency-key already exists! " +
					"The suite is not a fully featured API server and does not fully support " +
					"idempotency keys. Tests will fail if the suite receives repeated requests or multiple requests with different" +
				" idempotency keys. Please contact the certification team if this is a blocking issue for you.");
		} else {
			log("x-idempotency-key does not exist.");
			return env;
		}
	}

}
