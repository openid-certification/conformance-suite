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
import net.openid.conformance.authzen.condition.ChangeAuthzenSearchRequestPageLimit;
import net.openid.conformance.authzen.condition.EnsureAuthzenSearchResponseValsMatchExpectedVals;
import net.openid.conformance.authzen.condition.EnsureSearchFixtureEntitiesMatchBaseSearch;
import net.openid.conformance.authzen.condition.EnsureSearchPageTokenNotRepeated;
import net.openid.conformance.authzen.condition.EnsureSearchResultsMatchBaseSearch;
import net.openid.conformance.authzen.condition.ExtractAuthzenSearchExpectedResponse;
import net.openid.conformance.authzen.condition.WarnChangedPageRequestAccepted;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.TestFailureException;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractAuthzenPDPSearchTest extends AbstractAuthzenPDPTest {

	private static final int MAX_PAGES = 100;

	/**
	 * For the request that sends a page token with a changed page.limit: a 4xx is the error
	 * Section 8.2 asks for, and a 200 is checked by WarnChangedPageRequestAccepted; any other
	 * status fails the status check.
	 */
	private static final Set<Integer> CHANGED_PAGE_REQUEST_STATUSES = IntStream.concat(IntStream.of(200), IntStream.range(400, 500))
		.boxed().collect(Collectors.toUnmodifiableSet());

	private boolean sendingChangedPageRequest;

	private boolean sendingBaseSearch;

	/**
	 * Whether the module sends the token of the first page back with a changed page.limit,
	 * instead of following the pages (Section 8.2: the parameters of a request with a token MUST
	 * be identical to the preceding request's, and "PDPs SHOULD return an error when any entity
	 * or parameter has been changed"). Such a module's payload sets page.limit to 1.
	 */
	protected boolean sendsPageTokenWithChangedLimit() {
		return false;
	}

	/**
	 * The base search of a module whose search adds one member to it, as certification scenario
	 * 7.2.2, 7.2.3, 7.3.2, 7.3.3 and 7.4.2 name it.
	 *
	 * @param change the member the module's search adds, as named in messages
	 * @param removeChange removes that member from a copy of the module's request
	 * @param fixtureEntitiesOnly whether only the fixture entities, the expected results, are
	 *        compared instead of the whole result sets
	 * @param requirements the sections the comparison cites; the scenario has no requirement prefix
	 */
	public record BaseSearch(String change, UnaryOperator<JsonObject> removeChange, boolean fixtureEntitiesOnly,
		List<String> requirements) {
	}

	/**
	 * The base search to send and compare results with, or null, the default, to send none. The
	 * module then first sends the base search, following every page, and compares the two
	 * result sets at WARNING.
	 */
	protected BaseSearch getBaseSearch() {
		return null;
	}

	@Override
	protected JsonObject parseRequest() {
		JsonObject request = super.parseRequest();
		return sendingBaseSearch ? getBaseSearch().removeChange().apply(request) : request;
	}

	/**
	 * Override to supply the results a successful search is matched against (e.g.
	 * {"results": [ ... ]}). Modules that expect only an error status never match results, so
	 * they need not override it.
	 */
	protected String getExpectedSearchResponseJson() {
		return null;
	}

	private String requireExpectedSearchResponseJson() {
		String expectedJson = getExpectedSearchResponseJson();
		if (expectedJson == null) {
			throw new TestFailureException(getId(), getName() + " accepts HTTP 200 but defines no expected search results");
		}
		return expectedJson;
	}

	/**
	 * A search module matches the results of every page together, in {@link #runPaginatedSearchLoop}
	 * or {@link #performIdempotencyIteration}, never a single response.
	 */
	@Override
	protected void validateAuthApiEndpointResponse() {
		throw new TestFailureException(getId(), "Search results are matched once every page has been fetched, not per response");
	}

	/**
	 * A PDP may paginate a search response without being asked to (Section 8.2.2: "if a response
	 * does not contain the entire result set, it MUST include this object"), so every module that
	 * accepts a successful search follows the {@code page.next_token} chain before matching the
	 * results. Modules expecting only an error status make a single request.
	 */
	@Override
	protected void performAuthzenApiFlow() {
		if (sendsPageTokenWithChangedLimit()) {
			sendPageTokenWithChangedLimit();
		} else if (getAcceptableHttpStatusCodes().contains(200)) {
			runPaginatedSearchLoop();
		} else {
			super.performAuthzenApiFlow();
		}
	}

	/**
	 * Requests the first page, then sends its {@code page.next_token} with page.limit changed
	 * to 2. Pagination is optional (Section 8.2), so the test is skipped when the PDP returns
	 * the whole result set in one response.
	 */
	private void sendPageTokenWithChangedLimit() {
		eventLog.startBlock("Request the first page");
		try {
			createAuthzenApiRequest();
			performSingleApiRequest();
			if (!responseStatusIs200()) {
				// The status check has already failed, and there is no page token to send.
				throw new TestFailureException(getId(), "The PDP did not answer the first page with HTTP 200, so there is no page token to send with a changed limit");
			}
			processAuthApiEndpointResponse();
		} finally {
			eventLog.endBlock();
		}

		if (Strings.isNullOrEmpty(env.getString("authzen_search_endpoint_request_page_token"))) {
			fireTestSkipped("The PDP returned the whole result set in one response, so it does not paginate this search and there is no page token to send with a changed limit. Pagination is optional (Section 8.2).");
		}

		eventLog.startBlock("Send the page token with a changed limit");
		try {
			sendingChangedPageRequest = true;
			createAuthzenApiRequest();
			if (sendRawRequest()) {
				// The request builders add the page token; a raw request carries only the payload.
				callAndStopOnFailure(AddAuthzenSearchPageTokenToRequest.class, "AUTHZEN-8.2");
			}
			callAndStopOnFailure(new ChangeAuthzenSearchRequestPageLimit(2), "AUTHZEN-8.2", "AUTHZEN-8.2.1");
			performSingleApiRequest();
			callAndContinueOnFailure(WarnChangedPageRequestAccepted.class, ConditionResult.WARNING, "AUTHZEN-8.2", "AUTHZEN-8.2.1");
		} finally {
			sendingChangedPageRequest = false;
			eventLog.endBlock();
		}

		performPostApiFlow();
	}

	@Override
	protected Set<Integer> getAcceptableHttpStatusCodes() {
		return sendingChangedPageRequest ? CHANGED_PAGE_REQUEST_STATUSES : super.getAcceptableHttpStatusCodes();
	}

	@Override
	protected String[] expectedStatusRequirements(Set<Integer> acceptable) {
		return sendingChangedPageRequest ? new String[] {"AUTHZEN-8.2", "AUTHZEN-8.2.1", "AUTHZEN-10.1.2"} : super.expectedStatusRequirements(acceptable);
	}

	/**
	 * A response with a non-empty {@code page.next_token} leaves the token in the environment,
	 * and the request builder adds it to the next request as {@code page.token}. Each idempotency
	 * iteration must ask for the first page again, and gathers its own results and tokens.
	 */
	@Override
	protected void resetStateBetweenIdenticalRequests() {
		env.removeNativeValue("authzen_search_endpoint_request_page_token");
		env.removeObject("authzen_search_endpoint_aggregated_results");
		env.removeObject("authzen_search_seen_page_tokens");
	}

	/**
	 * Each identical search follows every page, as {@link #runPaginatedSearchLoop} does, and
	 * leaves the results of all pages in {@code authzen_search_endpoint_aggregated_results} for
	 * the idempotency comparison.
	 */
	@Override
	protected void performIdempotencyIteration() {
		if (fetchAllSearchPages()) {
			throw new TestFailureException(getId(),
				"The PDP did not answer the search with HTTP 200, so its results cannot be compared across identical requests");
		}
		matchAggregatedSearchResults();
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
		// The results are aggregated from the first page answered with HTTP 200, so they are absent
		// when the search itself failed, which the status check has already reported.
		if (!env.containsObject("authzen_search_endpoint_aggregated_results")) {
			eventLog.startBlock("Evaluate the search results");
			try {
				eventLog.log(getName(), "The PDP did not answer the search with HTTP 200, so no results were sent to the Access Evaluation API");
			} finally {
				eventLog.endBlock();
			}
			return;
		}
		JsonElement resultsElement = env.getElementFromObject("authzen_search_endpoint_aggregated_results", "results");
		if (resultsElement == null || !resultsElement.isJsonArray() || resultsElement.getAsJsonArray().isEmpty()) {
			eventLog.startBlock("Evaluate the search results");
			try {
				eventLog.log(getName(), "The search returned no results, so none were sent to the Access Evaluation API");
			} finally {
				eventLog.endBlock();
			}
			return;
		}
		JsonArray results = resultsElement.getAsJsonArray();
		int count = Math.min(results.size(), MAX_RESULTS_TO_EVALUATE);
		if (results.size() > count) {
			eventLog.startBlock("Evaluate the search results");
			try {
				eventLog.log(getName(), "The search returned " + results.size() + " results; the first " + count
					+ " are sent to the Access Evaluation API");
			} finally {
				eventLog.endBlock();
			}
		}
		for (int i = 0; i < count; i++) {
			// Every page's results are checked to be objects, stopping the test otherwise, before
			// they are aggregated.
			JsonObject result = results.get(i).getAsJsonObject();
			eventLog.startBlock("Evaluate search result " + (i + 1) + " of " + count);
			try {
				callAndStopOnFailure(new CreateAuthzenEvaluationRequestForSearchResult(searchRequest, searchedEntity, result), "AUTHZEN-8.1");
				callAndStopOnFailure(SetAuthzenApiEndpointToAccessEvaluationEndpoint.class);
				checkEndpointPathContainsV1();
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
	 * against the expected set. Log blocks do not nest, so each page has a block of its own,
	 * as does the matching; each is closed in a finally, so it closes even when a condition
	 * throws.
	 */
	protected void runPaginatedSearchLoop() {
		BaseSearch baseSearch = getBaseSearch();
		boolean compareWithBaseSearch = baseSearch != null && fetchBaseSearchResults();
		if (!fetchAllSearchPages()) {
			matchAggregatedSearchResults();
			if (compareWithBaseSearch) {
				compareWithBaseSearch(baseSearch);
			}
		}
		performPostApiFlow();
	}

	/**
	 * Sends the base search, following every page, and keeps its results in
	 * {@code authzen_search_base_results}. Its status is checked like any search's.
	 *
	 * @return false when the PDP did not answer the base search with HTTP 200, so there is nothing to compare
	 */
	private boolean fetchBaseSearchResults() {
		sendingBaseSearch = true;
		try {
			if (fetchAllSearchPages()) {
				return false;
			}
			env.putObject("authzen_search_base_results", env.getObject("authzen_search_endpoint_aggregated_results").deepCopy());
			return true;
		} finally {
			sendingBaseSearch = false;
			resetStateBetweenIdenticalRequests();
		}
	}

	/**
	 * Certification scenario 4.6 fixes only the fixture decisions and lets a PDP have other
	 * policies, which may depend on variables such as time, so a difference is a warning. The
	 * module's expected results, the fixture entities, are matched at FAILURE.
	 */
	private void compareWithBaseSearch(BaseSearch baseSearch) {
		eventLog.startBlock("Compare with the base search");
		try {
			env.putString("authzen_search_base_change", baseSearch.change());
			callAndContinueOnFailure(baseSearch.fixtureEntitiesOnly()
					? EnsureSearchFixtureEntitiesMatchBaseSearch.class
					: EnsureSearchResultsMatchBaseSearch.class,
				ConditionResult.WARNING, baseSearch.requirements().toArray(new String[0]));
		} finally {
			eventLog.endBlock();
		}
	}

	/**
	 * Sends the search request and then one request per {@code page.next_token}, each in its own
	 * log block, adding each page's results to {@code authzen_search_endpoint_aggregated_results}.
	 * A later page that is not a 200 fails the test.
	 *
	 * @return true when the first response is not a 200, so there are no results to match
	 */
	private boolean fetchAllSearchPages() {
		int pageNum = 0;
		do {
			pageNum++;
			if (pageNum > MAX_PAGES) {
				throw new TestFailureException(getId(),
					"Pagination cap of " + MAX_PAGES + " pages exceeded — server may be returning unbounded pages");
			}
			eventLog.startBlock((sendingBaseSearch ? "Base search, page " : "Page ") + pageNum);
			try {
				createAuthzenApiRequest();
				if (pageNum > 1 && sendRawRequest()) {
					callAndStopOnFailure(AddAuthzenSearchPageTokenToRequest.class, "AUTHZEN-8.2");
				}
				performSingleApiRequest();
				if (!responseStatusIs200()) {
					if (pageNum == 1) {
						return true;
					}
					requireLaterPageStatus200(pageNum);
				}
				processAuthApiEndpointResponse();
				callAndStopOnFailure(EnsureSearchPageTokenNotRepeated.class, "AUTHZEN-8.2");
				callAndStopOnFailure(AggregateAuthzenSearchResults.class);
			} finally {
				eventLog.endBlock();
			}
		} while (!Strings.isNullOrEmpty(env.getString("authzen_search_endpoint_request_page_token")));

		return false;
	}

	/**
	 * A later page answers the same search, sent with the token the PDP returned (Section 8.2), so
	 * it must be a 200 whichever error statuses the first request may get. For a module that only
	 * accepts 200 the status check has already failed; otherwise the status is checked here.
	 */
	private void requireLaterPageStatus200(int pageNum) {
		if (!getAcceptableHttpStatusCodes().equals(Set.of(200))) {
			env.mapKey("endpoint_response", "authzen_api_endpoint_response");
			try {
				// Section 10.1: "A successful response is an HTTPS response with a status code of 200".
				callAndStopOnFailure(EnsureHttpStatusCodeIs200.class, "AUTHZEN-10.1");
			} finally {
				env.unmapKey("endpoint_response");
			}
		}
		throw new TestFailureException(getId(), "The PDP did not answer page " + pageNum
			+ " of the search with HTTP 200, so the search results cannot be checked");
	}

	/**
	 * Matches the results of every page together against the expected results, in a log block of
	 * its own.
	 */
	private void matchAggregatedSearchResults() {
		eventLog.startBlock("Match the search results");
		try {
			callAndContinueOnFailure(new ExtractAuthzenSearchExpectedResponse(requireExpectedSearchResponseJson()), ConditionResult.FAILURE);
			env.mapKey("authzen_search_endpoint_response", "authzen_search_endpoint_aggregated_results");
			try {
				// The expected entities come from the certification fixture (scenario 4.5), not from the
				// Authorization API specification, and the scenario has no requirement link prefix.
				callAndContinueOnFailure(EnsureAuthzenSearchResponseValsMatchExpectedVals.class, ConditionResult.FAILURE);
			} finally {
				env.unmapKey("authzen_search_endpoint_response");
			}
		} finally {
			eventLog.endBlock();
		}
	}
}
