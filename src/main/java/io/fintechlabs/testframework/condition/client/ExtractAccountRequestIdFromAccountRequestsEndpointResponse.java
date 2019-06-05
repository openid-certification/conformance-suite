package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractAccountRequestIdFromAccountRequestsEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "account_requests_endpoint_response")
	@PostEnvironment(strings = "account_request_id")
	public Environment evaluate(Environment env) {
		String accountRequestId;

		Integer obApiVersion = env.getInteger("ob_api_version");
		if (obApiVersion == null) {
			throw error("ob_api_version missing from environment");
		}

		if (obApiVersion == 2) {
			accountRequestId = env.getString("account_requests_endpoint_response", "Data.AccountRequestId");
		} else if (obApiVersion == 3) {
			accountRequestId = env.getString("account_requests_endpoint_response", "Data.ConsentId");
		} else {
			throw error("ob_api_version "+obApiVersion+" not supported");
		}
		if (Strings.isNullOrEmpty(accountRequestId)) {
			throw error("Couldn't find account request ID");
		}

		env.putString("account_request_id", accountRequestId);

		logSuccess("Extracted the account request ID", args("account_request_id", accountRequestId));

		return env;
	}

}
