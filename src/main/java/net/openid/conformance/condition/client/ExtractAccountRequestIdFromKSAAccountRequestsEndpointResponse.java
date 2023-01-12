package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAccountRequestIdFromKSAAccountRequestsEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "account_requests_endpoint_response")
	@PostEnvironment(strings = "account_request_id")
	public Environment evaluate(Environment env) {


		String path = "Data.ConsentId";

		String accountRequestId = env.getString("account_requests_endpoint_response", path);
		if (Strings.isNullOrEmpty(accountRequestId)) {
			throw error("Couldn't find consent id in Data.ConsentId");
		}

		env.putString("account_request_id", accountRequestId);

		logSuccess("Extracted the account request ID", args("account_request_id", accountRequestId));

		return env;
	}

}
