package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateEntityStatementIss extends AbstractCondition {

	@Override
	@PostEnvironment(required = { "server", "config" } )
	public Environment evaluate(Environment env) {
		JsonElement iss = env.getElementFromObject("server", "iss");

		if (iss == null || iss.isJsonObject()) {
			throw error("iss is missing from entity statement");
		}

		String entityStatementUrl = env.getString("config", "server.entityStatementUrl");

		String issuerUrl = OIDFJSON.getString(iss);

		final String removingPartInUrl = ".well-known/openid-federation";
		if (entityStatementUrl.endsWith(removingPartInUrl)) {
			entityStatementUrl = entityStatementUrl.substring(0, entityStatementUrl.length() - removingPartInUrl.length());
		}

		if (!removeTrailingSlash(issuerUrl).equals(removeTrailingSlash(entityStatementUrl))) {
			throw error("iss listed in the entity statement is not consistent with the location the entity statement was retrieved from. These must match to prevent impersonation attacks.", args("entity_statement_url", entityStatementUrl, "iss", issuerUrl));
		}

		logSuccess("iss is consistent with the entity statement endpoint URL");

		return env;
	}

	private String removeTrailingSlash(String url) {
		if (url.endsWith("/")) {
			return url.substring(0, url.length() - 1);
		}
		return url;
	}
}
