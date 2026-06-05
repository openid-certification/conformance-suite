package net.openid.conformance.authzen;

import com.google.common.base.Strings;
import net.openid.conformance.authzen.condition.AggregateAuthzenSearchResults;
import net.openid.conformance.authzen.condition.EnsureAuthzenSearchResponseValsMatchExpectedVals;
import net.openid.conformance.authzen.condition.EnsureValidSearchResponse;
import net.openid.conformance.authzen.condition.EnsureValidSearchResponsePage;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointSearchResponse;
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

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointSearchResponse.class, "AUTHZEN-8.3");
		callAndStopOnFailure(EnsureValidSearchResponse.class, "AUTHZEN-8.3");
		callAndStopOnFailure(EnsureValidSearchResponsePage.class, "AUTHZEN-8.2.2", "AUTHZEN-8.3");
	}

	/**
	 * Paginated search loop shared by every per-axis paginated base
	 * ({@code AbstractAuthzenPDPPaginated{Subject,Resource,Action}SearchTest}).
	 * Follows the {@code page.next_token} chain (capped at {@code MAX_PAGES}),
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
			do {
				pageNum++;
				if (pageNum > MAX_PAGES) {
					throw new TestFailureException(getId(),
						"Pagination cap of " + MAX_PAGES + " pages exceeded — server may be returning unbounded pages");
				}
				eventLog.startBlock("Page " + pageNum);
				try {
					createAuthzenApiRequest();
					performSingleApiRequest();
					processAuthApiEndpointResponse();
					callAndStopOnFailure(AggregateAuthzenSearchResults.class);
				} finally {
					eventLog.endBlock();
				}
			} while (!Strings.isNullOrEmpty(env.getString("authzen_search_endpoint_request_page_token")));

			// Use accumulated results for matching
			env.mapKey("authzen_search_endpoint_response", "authzen_search_endpoint_aggregated_results");
			callAndContinueOnFailure(EnsureAuthzenSearchResponseValsMatchExpectedVals.class, ConditionResult.FAILURE, "AUTHZEN-8.3");
			env.unmapKey("authzen_search_endpoint_response");

			performPostApiFlow();
		} finally {
			eventLog.endBlock();
		}
	}
}
