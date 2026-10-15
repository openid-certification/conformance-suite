package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Captures the search results gathered across every page by {@link AggregateAuthzenSearchResults}
 * on the first iteration of an idempotency test loop, for
 * {@link EnsureAuthzenSearchResultsMatchIdempotencyCheck} to compare later iterations against.
 */
public class CaptureAuthzenSearchResultsForIdempotencyCheck extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authzen_search_endpoint_aggregated_results")
	@PostEnvironment(required = "authzen_idempotency_first_search_results")
	public Environment evaluate(Environment env) {
		JsonElement results = env.getElementFromObject("authzen_search_endpoint_aggregated_results", "results");
		if (results == null || !results.isJsonArray()) {
			throw error("No search results to capture for the idempotency check",
				args("aggregated", env.getObject("authzen_search_endpoint_aggregated_results")));
		}
		JsonObject captured = new JsonObject();
		captured.add("results", results.deepCopy());
		env.putObject("authzen_idempotency_first_search_results", captured);
		logSuccess("Captured the search results of the first request for the idempotency check", args("results", results));
		return env;
	}
}
