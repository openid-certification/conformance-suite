package net.openid.conformance.authzen;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.authzen.condition.CallAuthzenApiEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.authzen.condition.CreateAuthzenEvaluationRequestForSearchResult;
import net.openid.conformance.authzen.condition.EnsureSearchResultIsPermitted;
import net.openid.conformance.authzen.condition.EnsureValidDecisionResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointDecisionResponse;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToAccessEvaluationEndpoint;
import net.openid.conformance.authzen.condition.AddAuthzenSearchPageTokenToRequest;
import net.openid.conformance.authzen.condition.AggregateAuthzenSearchResults;
import net.openid.conformance.authzen.condition.EnsureAuthzenSearchResponseValsMatchExpectedVals;
import net.openid.conformance.authzen.condition.EnsureSearchPageTokenNotRepeated;
import net.openid.conformance.authzen.condition.ExtractAuthzenSearchExpectedResponse;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.TestFailureException;

public abstract class AbstractAuthzenPDPSearchTest extends AbstractAuthzenPDPTest {

	private static final int MAX_PAGES = 100;

	protected abstract String getExpectedSearchResponseJson();

	@Override
	protected void validateAuthApiEndpointResponse() {
		callAndContinueOnFailure(new ExtractAuthzenSearchExpectedResponse(getExpectedSearchResponseJson()), ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureAuthzenSearchResponseValsMatchExpectedVals.class, ConditionResult.FAILURE, "AUTHZEN-8.3");
	}

	/**
	 * A PDP may paginate a search response without being asked to (Section 8.2.2: "if a response
	 * does not contain the entire result set, it MUST include this object"), so every module that
	 * accepts a successful search follows the {@code page.next_token} chain before matching the
	 * results. Modules expecting only an error status make a single request.
	 */
	@Override
	protected void performAuthzenApiFlow() {
		if (getAcceptableHttpStatusCodes().contains(200)) {
			runPaginatedSearchLoop();
		} else {
			super.performAuthzenApiFlow();
		}
	}

	/**
	 * A response with a non-empty {@code page.next_token} leaves the token in the environment,
	 * and the request builder adds it to the next request as {@code page.token}. Each idempotency
	 * iteration must ask for the first page again.
	 */
	@Override
	protected void resetStateBetweenIdenticalRequests() {
		env.removeNativeValue("authzen_search_endpoint_request_page_token");
	}

	/**
	 * Certification scenario 5.6 requires the same decision for repeated requests; it says
	 * nothing about search results, and Section 8.1 says they "may depend on other variables
	 * (such as time)". A search that returns different results is reported as a warning.
	 */
	@Override
	protected ConditionResult getIdempotencySeverity() {
		return ConditionResult.WARNING;
	}

	private static final int MAX_RESULTS_TO_EVALUATE = 10;

	/**
	 * Section 8.1: each search result "when subsequently used in an Access Evaluation API call,
	 * SHOULD result in a "decision": true response". Sends the first results accumulated by
	 * {@link #runPaginatedSearchLoop} to the Access Evaluation API, with the searched-for entity
	 * replaced by the result, and warns about each one that is not permitted.
	 *
	 * @param searchedEntity the request member the search fills in: subject, resource or action
	 */
	protected void evaluateSearchResultsWithAccessEvaluationApi(String searchedEntity) {
		JsonObject searchRequest = env.getObject("authzen_api_endpoint_request").deepCopy();
		JsonElement resultsElement = env.getElementFromObject("authzen_search_endpoint_aggregated_results", "results");
		if (resultsElement == null || !resultsElement.isJsonArray() || resultsElement.getAsJsonArray().isEmpty()) {
			eventLog.log(getName(), "The search returned no results, so none were sent to the Access Evaluation API");
			return;
		}
		JsonArray results = resultsElement.getAsJsonArray();
		int count = Math.min(results.size(), MAX_RESULTS_TO_EVALUATE);
		if (results.size() > count) {
			eventLog.log(getName(), "The search returned " + results.size() + " results; the first " + count
				+ " are sent to the Access Evaluation API");
		}
		for (int i = 0; i < count; i++) {
			// Every page's results are checked to be objects, stopping the test otherwise, before
			// they are aggregated.
			JsonObject result = results.get(i).getAsJsonObject();
			eventLog.startBlock("Evaluate search result " + (i + 1) + " of " + count);
			try {
				callAndStopOnFailure(new CreateAuthzenEvaluationRequestForSearchResult(searchRequest, searchedEntity, result), "AUTHZEN-8.1");
				callAndStopOnFailure(SetAuthzenApiEndpointToAccessEvaluationEndpoint.class);
				addAuthenticationToAuthzenApiEndpoint();
				call(sequence(CallAuthzenApiEndpointAndVerifySuccessfulResponse.class));
				callAndStopOnFailure(ExtractAuthzenApiEndpointDecisionResponse.class, "AUTHZEN-6.2");
				callAndStopOnFailure(EnsureValidDecisionResponse.class, "AUTHZEN-6.2");
				callAndContinueOnFailure(EnsureSearchResultIsPermitted.class, ConditionResult.WARNING, "AUTHZEN-8.1");
			} finally {
				eventLog.endBlock();
			}
		}
	}

	// processAuthApiEndpointResponse() stays abstract from AbstractAuthzenPDPTest;
	// the three per-axis subclasses (Subject/Resource/Action) each provide their
	// own implementation citing the family-specific AUTHZEN-8.{4,5,6} tag.

	/**
	 * Paginated search loop used by every search module that accepts HTTP 200. When the first
	 * response is not a 200 the loop stops there and no results are matched: for a module that
	 * also accepts an error status the search was rejected, and for one that accepts only 200
	 * the status check has already failed.
	 * Follows the {@code page.next_token} chain, failing on a page that repeats an earlier
	 * page's token and results, and capped at {@code MAX_PAGES},
	 * aggregating results across pages, and matches the accumulated results
	 * against the expected set. Nested {@code eventLog} blocks (one outer,
	 * one per page) are wrapped in try/finally so block scopes always close
	 * even when a condition throws.
	 */
	protected void runPaginatedSearchLoop() {
		eventLog.startBlock("Make paginated request to API endpoint");
		try {
			// Extract the expected response once up front so per-page conditions can use it.
			callAndContinueOnFailure(new ExtractAuthzenSearchExpectedResponse(getExpectedSearchResponseJson()), ConditionResult.FAILURE);

			int pageNum = 0;
			boolean rejected = false;
			do {
				pageNum++;
				if (pageNum > MAX_PAGES) {
					throw new TestFailureException(getId(),
						"Pagination cap of " + MAX_PAGES + " pages exceeded — server may be returning unbounded pages");
				}
				eventLog.startBlock("Page " + pageNum);
				try {
					createAuthzenApiRequest();
					if (pageNum > 1 && sendRawRequest()) {
						callAndStopOnFailure(AddAuthzenSearchPageTokenToRequest.class, "AUTHZEN-8.2");
					}
					performSingleApiRequest();
					if (pageNum == 1 && !responseStatusIs200()) {
						rejected = true;
						break;
					}
					processAuthApiEndpointResponse();
					callAndStopOnFailure(EnsureSearchPageTokenNotRepeated.class, "AUTHZEN-8.2");
					callAndStopOnFailure(AggregateAuthzenSearchResults.class);
				} finally {
					eventLog.endBlock();
				}
			} while (!Strings.isNullOrEmpty(env.getString("authzen_search_endpoint_request_page_token")));

			if (!rejected) {
				// Use accumulated results for matching
				env.mapKey("authzen_search_endpoint_response", "authzen_search_endpoint_aggregated_results");
				callAndContinueOnFailure(EnsureAuthzenSearchResponseValsMatchExpectedVals.class, ConditionResult.FAILURE, "AUTHZEN-8.3");
				env.unmapKey("authzen_search_endpoint_response");
			}

			performPostApiFlow();
		} finally {
			eventLog.endBlock();
		}
	}
}
