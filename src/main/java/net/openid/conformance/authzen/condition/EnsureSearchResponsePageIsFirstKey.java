package net.openid.conformance.authzen.condition;

import com.google.gson.JsonObject;
import java.util.Map;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Section 8.3 RECOMMENDED that the `page` member be the first key of a
 * search response. Gson's JsonObject preserves the insertion order of the parsed
 * JSON, so the first entry in the iteration order matches the wire order. This
 * check throws on violation; the caller decides whether to surface as WARNING.
 */
public class EnsureSearchResponsePageIsFirstKey extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject response = env.getObject("authzen_search_endpoint_response");
		if (!response.has("page")) {
			logSuccess("Response has no `page` member — ordering check trivially satisfied");
			return env;
		}
		Map.Entry<String, com.google.gson.JsonElement> first = response.entrySet().iterator().next();
		if (!"page".equals(first.getKey())) {
			throw error("Section 8.3 RECOMMENDED that the search response `page` member be the first key",
				args("first_key", first.getKey(), "response", response));
		}
		logSuccess("Search response begins with `page`");
		return env;
	}
}
