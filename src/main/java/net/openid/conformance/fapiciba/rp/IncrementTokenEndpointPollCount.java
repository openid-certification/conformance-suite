package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class IncrementTokenEndpointPollCount extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		Integer pollCount = env.getInteger("token_poll_count");
		if(pollCount == null) {
			pollCount = 0;
		}
		pollCount++;
		env.putInteger("token_poll_count", pollCount);

		logSuccess("Incremented token endpoint poll count", args("token_poll_count", pollCount));
		return env;

	}

}
