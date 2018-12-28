package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;

import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

public class ExtractAccountRequestIdFromAccountRequestsEndpointResponse extends AbstractCondition {

	public ExtractAccountRequestIdFromAccountRequestsEndpointResponse(String testId, TestInstanceEventLog log, ConditionResult conditionResultOnFailure, String... requirements) {
		super(testId, log, conditionResultOnFailure, requirements);
	}

	/* (non-Javadoc)
	 * @see io.fintechlabs.testframework.condition.Condition#evaluate(io.fintechlabs.testframework.testmodule.Environment)
	 */
	@Override
	@PreEnvironment(required = "account_requests_endpoint_response")
	@PostEnvironment(strings = "account_request_id")
	public Environment evaluate(Environment env) {

		String accountRequestId = env.getString("account_requests_endpoint_response", "Data.AccountRequestId");
		if (Strings.isNullOrEmpty(accountRequestId)) {
			throw error("Couldn't find account request ID");
		}

		env.putString("account_request_id", accountRequestId);

		logSuccess("Extracted the account request ID", args("account_request_id", accountRequestId));

		return env;
	}

}
