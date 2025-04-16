package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;

public class AddInvalidExpToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		long exp = Instant.now().minusSeconds(5 * 60).getEpochSecond();
		env.putLong("request_object_claims", "exp", exp);

		logSuccess("Added invalid exp claim", args("exp", exp));

		return env;
	}
}
