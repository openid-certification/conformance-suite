package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractAccountRequestIdFromAccountRequestsEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "account_requests_endpoint_response")
	@PostEnvironment(strings = "account_request_id")
	public Environment evaluate(Environment env) {

		Integer obApiVersion = env.getInteger("ob_api_version");
		if (obApiVersion == null) {
			throw error("ob_api_version missing from environment");
		}

		String path;
		if (obApiVersion == 2) {
			path = "Data.AccountRequestId";
		} else if ((obApiVersion == 3) || (obApiVersion == 4)) {
			path = "Data.ConsentId";
		} else {
			throw error("ob_api_version "+obApiVersion+" not supported");
		}
		String accountRequestId = env.getString("account_requests_endpoint_response", path);
		if (Strings.isNullOrEmpty(accountRequestId)) {
			throw error("Couldn't find account request ID. (The location this is returned in varies depending "+
				"on which version of the OpenBanking UK API is in use - the version must be included in the resource "+
					"endpoint url as per the OpenBanking UK specs.)",
				args("obuk_api_version", obApiVersion,
					"path", path));
		}

		env.putString("account_request_id", accountRequestId);

		logSuccess("Extracted the account request ID", args("account_request_id", accountRequestId));

		return env;
	}

}
