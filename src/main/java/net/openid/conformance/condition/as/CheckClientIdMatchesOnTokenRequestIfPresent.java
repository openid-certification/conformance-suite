package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckClientIdMatchesOnTokenRequestIfPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"token_endpoint_request", "client"})
	public Environment evaluate(Environment env) {

		String clientId = env.getString("token_endpoint_request", "body_form_params.client_id");
		String expectedClientId = env.getString("client", "client_id");

		if (Strings.isNullOrEmpty(clientId)) {
			log("client_id not present, nothing to check");
			return env;
		}

		if (expectedClientId.equals(clientId)) {
			logSuccess("Extracted client_id matches the expected value", args("client_id", clientId));
			return env;
		}

		throw error("client_id on the request " + clientId + " does not match the expected one " + expectedClientId,
			args("expected", expectedClientId, "actual", clientId) );

	}
}
