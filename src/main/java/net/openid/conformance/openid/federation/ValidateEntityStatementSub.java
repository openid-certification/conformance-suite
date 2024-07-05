package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateEntityStatementSub extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "entity_statement_body", "config" } )
	public Environment evaluate(Environment env) {
		JsonElement sub = env.getElementFromObject("entity_statement_body", "sub");

		if (sub == null || sub.isJsonObject()) {
			throw error("sub is missing from entity statement");
		}

		String entityStatementUrl = env.getString("config", "server.entityStatementUrl");

		String subUrl = OIDFJSON.getString(sub);

		final String removingPartInUrl = ".well-known/openid-federation";
		if (entityStatementUrl.endsWith(removingPartInUrl)) {
			entityStatementUrl = entityStatementUrl.substring(0, entityStatementUrl.length() - removingPartInUrl.length());
		}

		if (!removeTrailingSlash(subUrl).equals(removeTrailingSlash(entityStatementUrl))) {
			throw error("sub listed in the entity statement is not consistent with the location the entity statement was retrieved from. These must match to prevent impersonation attacks.", args("entity_statement_url", entityStatementUrl, "sub", subUrl));
		}

		logSuccess("sub is consistent with the entity statement endpoint URL", args("sub", sub));

		return env;
	}

	private String removeTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
