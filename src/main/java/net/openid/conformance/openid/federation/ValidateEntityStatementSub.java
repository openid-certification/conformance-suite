package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateEntityStatementSub extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "entity_statement_sub" } )
	public Environment evaluate(Environment env) {
		String sub = env.getString("entity_statement_sub");

		if (sub == null) {
			throw error("sub is missing", args("sub", sub));
		}

		String entityStatementUrl = env.getString("entity_statement_url");

		final String removingPartInUrl = ".well-known/openid-federation";
		if (entityStatementUrl.endsWith(removingPartInUrl)) {
			entityStatementUrl = entityStatementUrl.substring(0, entityStatementUrl.length() - removingPartInUrl.length());
		}

		if (!removeTrailingSlash(sub).equals(removeTrailingSlash(entityStatementUrl))) {
			throw error("sub listed in the entity statement is not consistent with the location the entity statement was retrieved from. These must match to prevent impersonation attacks.", args("entity_statement_url", entityStatementUrl, "sub", sub));
		}

		logSuccess("sub is consistent with the entity statement endpoint URL", args("expected", entityStatementUrl, "actual", sub));

		return env;
	}

	private String removeTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
