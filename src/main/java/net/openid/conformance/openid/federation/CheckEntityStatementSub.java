package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class CheckEntityStatementSub extends AbstractCondition {

	@Override
	@PostEnvironment(required = { "server", "config" } )
	public Environment evaluate(Environment env) {
		JsonElement sub = env.getElementFromObject("server", "sub");

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

		logSuccess("sub is consistent with the entity statement endpoint URL");

		return env;
	}

	private String removeTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
