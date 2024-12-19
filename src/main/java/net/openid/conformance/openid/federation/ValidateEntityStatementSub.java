package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class ValidateEntityStatementSub extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String sub = env.getString("federation_response_sub");
		String expectedSub = env.getString("expected_sub");

		if (!removeTrailingSlash(sub).equals(removeTrailingSlash(expectedSub))) {
			throw error("sub listed in the entity statement is not consistent with the location the entity statement was retrieved from. " +
				"These must match to prevent impersonation attacks.", args("expected", expectedSub, "actual", sub));
		}

		logSuccess("sub is consistent with the entity statement endpoint URL", args("expected", expectedSub, "actual", sub));

		return env;
	}

	private String removeTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
