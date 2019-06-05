package io.fintechlabs.testframework.condition.as;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class EnsureNoClientAssertionSentToTokenEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(required = "token_endpoint_request")
	public Environment evaluate(Environment env) {

		String clientAssertionString = env.getString("token_endpoint_request", "params.client_assertion");
		if (Strings.isNullOrEmpty(clientAssertionString)) {
			logSuccess("Client did not send a client_assertion to token endpoint");
			return env;
		} else {
			throw error("Client assertion should not exist in request parameters");
		}
	}
}
