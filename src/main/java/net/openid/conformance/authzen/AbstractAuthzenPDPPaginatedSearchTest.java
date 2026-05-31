package net.openid.conformance.authzen;

import com.google.common.base.Strings;
import net.openid.conformance.authzen.condition.AggregateAuthzenSearchResults;
import net.openid.conformance.authzen.condition.EnsureAuthzenSearchResponseValsMatchExpectedVals;
import net.openid.conformance.authzen.condition.ExtractAuthzenSearchExpectedResponse;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.TestFailureException;

/**
 * Search test that follows pagination across multiple pages.
 * Aggregated results across all pages must include the expected set.
 *
 * Subclasses extend this in place of {@link AbstractAuthzenPDPSearchTest} when the test exercises
 * a paginated response. Single-page tests should continue to extend {@link AbstractAuthzenPDPSearchTest}.
 */
public abstract class AbstractAuthzenPDPPaginatedSearchTest extends AbstractAuthzenPDPSearchTest {

	private static final int MAX_PAGES = 100;

	@Override
	protected void performAuthzenApiFlow() {
		eventLog.startBlock("Make paginated request to API endpoint");

		// Extract the expected response once up front so per-page conditions can use it.
		callAndContinueOnFailure(new ExtractAuthzenSearchExpectedResponse(getExpectedSearchResponseJson()), ConditionResult.FAILURE);

		int pageNum = 0;
		do {
			pageNum++;
			if (pageNum > MAX_PAGES) {
				throw new TestFailureException(getId(),
					"Pagination cap of " + MAX_PAGES + " pages exceeded — server may be returning unbounded pages");
			}
			eventLog.startBlock("Page " + pageNum);
			createAuthzenApiRequest();
			callAuthApiEndpointRequest();
			processAuthApiEndpointResponse();
			callAndStopOnFailure(AggregateAuthzenSearchResults.class);
			eventLog.endBlock();
		} while (!Strings.isNullOrEmpty(env.getString("authzen_search_endpoint_request_page_token")));

		// Use accumulated results for matching
		env.mapKey("authzen_search_endpoint_response", "authzen_search_endpoint_aggregated_results");
		callAndContinueOnFailure(EnsureAuthzenSearchResponseValsMatchExpectedVals.class, ConditionResult.FAILURE, "AUTHZEN-8.3");
		env.unmapKey("authzen_search_endpoint_response");

		performPostApiFlow();
		eventLog.endBlock();
	}

	@Override
	protected void validateAuthApiEndpointResponse() {
		// Validation is performed inside the paginated flow above.
	}
}
