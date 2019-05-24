package io.fintechlabs.testframework.condition.client;

import com.google.common.base.Strings;
import io.fintechlabs.testframework.condition.AbstractCondition;
import io.fintechlabs.testframework.condition.PostEnvironment;
import io.fintechlabs.testframework.condition.PreEnvironment;
import io.fintechlabs.testframework.testmodule.Environment;

public class CreateInvalidRedirectTargetEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(strings = "invalid_redirect_target_endpoint_uri")
	public Environment evaluate(Environment in) {
		String baseUrl = in.getString("base_url");

		if (Strings.isNullOrEmpty(baseUrl)) {
			throw error("Base URL was null or empty");
		}

		// calculate the redirect URI based on our given base URL
		String invalidRedirectionUri = baseUrl + "/invalid-redirect-target-endpoint";
		in.putString("invalid_redirect_target_endpoint_uri", invalidRedirectionUri);

		logSuccess("Created invalid redirect target endpoint URI",
			args("invalid_redirect_target_endpoint_uri", invalidRedirectionUri));

		return in;
	}

}
