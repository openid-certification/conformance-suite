package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-request-with-page-limit",
	displayName = "AuthZEN Action Search API - Section 8.2.1: Request with page limit",
	summary = "Section 8.2.1 request with a `page.limit` field (certification scenario 7.5.1). The PDP MUST accept the request. Pagination is optional (Section 8.2, certification scenario 7.5.4): a PDP that does not paginate returns all results in one response, and a PDP that paginates returns at most `limit` results per page. The harness follows `page.next_token` across pages, and the accumulated results MUST include read and write. Exceeding the limit while paginating is reported as a warning, as Section 8.2.1 defines the limit without a MUST.\n" + AuthzenPDPActionSearchRequestWithPageLimitTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchRequestWithPageLimitTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"resource": { "type": "record", "id": "record-1" },
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
					{ "name": "read" },
					{ "name": "write" }
				]
			}
			""";
	}
}
