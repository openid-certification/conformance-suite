package net.openid.conformance.authzen;

import com.google.common.base.Strings;
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
