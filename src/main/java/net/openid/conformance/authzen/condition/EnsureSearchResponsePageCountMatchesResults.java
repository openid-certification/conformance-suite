package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.math.BigDecimal;

/**
 * Section 8.2.2: `page.count` is "the number of results included in this response",
 * so when present it must equal the length of `results`. Runs after
 * {@link EnsureValidSearchResponsePage}, which checks that count is a non-negative integer.
 */
public class EnsureSearchResponsePageCountMatchesResults extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_response")
	public Environment evaluate(Environment env) {
		JsonObject searchResponse = env.getObject("authzen_search_endpoint_response");
		JsonElement count = env.getElementFromObject("authzen_search_endpoint_response", "page.count");
		if (count == null) {
			log("Search response page object has no count");
			return env;
		}
		if (!count.isJsonPrimitive() || !count.getAsJsonPrimitive().isNumber()) {
			throw error("page.count is not a number", args("count", count));
		}
		int resultsSize = searchResponse.getAsJsonArray("results").size();
		// Compared exactly: reading count as an int would wrap a value beyond the int range
		// (4294967297 becomes 1) and could match the number of results by accident.
		BigDecimal countValue = new BigDecimal(OIDFJSON.getNumber(count).toString());
		if (countValue.compareTo(BigDecimal.valueOf(resultsSize)) != 0) {
			throw error("page.count does not equal the number of results in the response",
				args("count", count, "results_size", resultsSize));
		}
		logSuccess("page.count equals the number of results in the response", args("count", count));
		return env;
	}
}
