package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Fails when a page of a paginated search returns a `next_token` that an earlier page of the
 * same search already returned, together with the same results as that page: the PEP sends the
 * same request with the same token again (Section 8.2), so it would go round the same pages
 * forever. Tokens are opaque, so a PDP that keeps the position on the server may return the same
 * token while the results move on; that passes, and the caller's page cap bounds the search.
 * Records each page's token and results under {@code authzen_search_seen_page_tokens}.
 */
public class EnsureSearchPageTokenNotRepeated extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_response")
	public Environment evaluate(Environment env) {
		String token = env.getString("authzen_search_endpoint_request_page_token");
		if (Strings.isNullOrEmpty(token)) {
			logSuccess("This page signals no further pages");
			return env;
		}
		JsonElement resultsElem = env.getElementFromObject("authzen_search_endpoint_response", "results");
		JsonElement results = resultsElem == null ? JsonNull.INSTANCE : resultsElem.deepCopy();

		JsonObject seen = env.getObject("authzen_search_seen_page_tokens");
		if (seen == null) {
			seen = new JsonObject();
			seen.add("pages", new JsonArray());
		}
		JsonArray pages = seen.getAsJsonArray("pages");
		boolean tokenSeen = false;
		for (JsonElement pageElem : pages) {
			JsonObject page = pageElem.getAsJsonObject();
			if (!token.equals(OIDFJSON.getString(page.get("token")))) {
				continue;
			}
			tokenSeen = true;
			if (results.equals(page.get("results"))) {
				throw error("The PDP returned the same next_token and the same results as an earlier page of this search, so pagination would never end",
					args("next_token", token, "results", results));
			}
		}

		JsonObject page = new JsonObject();
		page.addProperty("token", token);
		page.add("results", results);
		pages.add(page);
		env.putObject("authzen_search_seen_page_tokens", seen);
		if (tokenSeen) {
			logSuccess("next_token was returned by an earlier page of this search, but with different results, so the PDP's position has moved on; tokens are opaque (Section 8.2)",
				args("next_token", token));
		} else {
			logSuccess("next_token has not been returned by an earlier page of this search", args("next_token", token));
		}
		return env;
	}
}
