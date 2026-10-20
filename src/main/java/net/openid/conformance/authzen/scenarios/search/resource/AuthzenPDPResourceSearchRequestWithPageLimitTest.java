package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-request-with-page-limit",
	displayName = "AuthZEN Resource Search API - Section 8.2.1: Request with page limit",
	summary = "Section 8.2.1 request with a `page.limit` field (certification scenario 7.5.1). The PDP MUST accept the request. Pagination is optional (Section 8.2, certification scenario 7.5.4): a PDP that does not paginate returns all results in one response, and a PDP that paginates returns at most `limit` results per page. The harness follows `page.next_token` across pages, and the accumulated results MUST include record-1. Exceeding the limit while paginating fails, as Section 8.2.1 defines it as the maximum number of results to return in the response. The fixture guarantees only record-1 for this search (certification scenario S2), so a PDP that paginates may still return it in one page, and no second page is then requested.\n" + AuthzenPDPResourceSearchRequestWithPageLimitTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchRequestWithPageLimitTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" },
			"page": {
				"limit": 1
			}
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return """
			{
				"results": [
					{ "type": "record", "id": "record-1" }
				]
			}
			""";
	}
}
