package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-request-with-page-limit",
	displayName = "Authzen Subject Search API - Section 8.3: Request with page limit",
	summary = "Section 8.3 request with a page.limit field. The PDP MUST accept the request. Results MUST include at least alice and bob over one or more pages.\n" + AuthzenPDPSubjectSearchRequestWithPageLimitTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchRequestWithPageLimitTest extends AbstractAuthzenPDPSubjectSearchTest {

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
