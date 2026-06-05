package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPPaginatedSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-request-with-page-limit",
	displayName = "Authzen Subject Search API - Section 8.3: Request with page limit",
	summary = "Section 8.3 request with a `page.limit` field. The PDP MUST accept the request. With `limit: 1`, a conformant PDP returns each result on its own page, so the harness follows the pagination across pages and the accumulated results MUST include alice and bob.\n" + AuthzenPDPSubjectSearchRequestWithPageLimitTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchRequestWithPageLimitTest extends AbstractAuthzenPDPPaginatedSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
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
					{ "type": "user", "id": "alice" },
					{ "type": "user", "id": "bob" }
				]
			}
			""";
	}
}
