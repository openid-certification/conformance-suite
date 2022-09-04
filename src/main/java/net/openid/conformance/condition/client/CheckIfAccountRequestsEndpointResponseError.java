package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckIfAccountRequestsEndpointResponseError extends AbstractCondition {

	@Override
	@PreEnvironment(required = "account_requests_endpoint_response")
	public Environment evaluate(Environment env) {

		if (!Strings.isNullOrEmpty(env.getString("account_requests_endpoint_response", "error"))) {
			throw error("Account requests endpoint error response", env.getObject("account_requests_endpoint_response"));
		} else {
			logSuccess("No error from account requests endpoint");
			return env;
		}

	}

}
