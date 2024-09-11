package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateEntityStatementIss extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "entity_statement_iss" } )
	public Environment evaluate(Environment env) {
		String iss = env.getString("entity_statement_iss");

		if (iss == null) {
			throw error("iss is missing", args("iss", iss));
		}

		String entityStatementUrl = env.getString("entity_statement_url");

		final String removingPartInUrl = ".well-known/openid-federation";
		if (entityStatementUrl.endsWith(removingPartInUrl)) {
			entityStatementUrl = entityStatementUrl.substring(0, entityStatementUrl.length() - removingPartInUrl.length());
		}

		if (!removeTrailingSlash(iss).equals(removeTrailingSlash(entityStatementUrl))) {
			throw error("iss listed in the entity statement is not consistent with the location the entity statement was retrieved from. " +
				"These must match to prevent impersonation attacks.", args("expected", entityStatementUrl, "actual", iss));
		}

		logSuccess("iss is consistent with the entity statement endpoint URL", args("expected", entityStatementUrl, "actual", iss));

		return env;
	}

	private String removeTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
