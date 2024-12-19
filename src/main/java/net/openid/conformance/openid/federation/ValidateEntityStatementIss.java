package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ValidateEntityStatementIss extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String iss = env.getString("federation_response_iss");
		String expectedIss = env.getString("expected_iss");

		if (!removeTrailingSlash(iss).equals(removeTrailingSlash(expectedIss))) {
			throw error("iss listed in the entity statement is not consistent with the location the entity statement was retrieved from. " +
				"These must match to prevent impersonation attacks.", args("expected", expectedIss, "actual", iss));
		}

		logSuccess("iss is consistent with the entity statement endpoint URL", args("expected", expectedIss, "actual", iss));

		return env;
	}

	private String removeTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
