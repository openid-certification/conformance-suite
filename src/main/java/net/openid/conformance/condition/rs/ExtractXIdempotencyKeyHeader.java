package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractXIdempotencyKeyHeader extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = "idempotency_key")
	public Environment evaluate(Environment env) {

		String header = env.getString("incoming_request", "headers.x-idempotency-key");
		if (Strings.isNullOrEmpty(header)) {
			throw error("Couldn't find x-idempotency-key header");
		} else {

			env.putString("idempotency_key", header);
			logSuccess("Found an x-idempotency-key header", args("idempotency_key", header));

			return env;

		}

	}

}
