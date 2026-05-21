package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-valid-action-search-request-with-non-empty-results",
	displayName = "Authzen Action Search API - Section 4.4.1: Valid action search request with non-empty results",
	summary = "Section 4.4.1 valid action search. The action field is omitted entirely from the request. Actions alice can perform on record-1 MUST include at least read and write.\n" + AuthzenPDPActionSearchValidActionSearchRequestWithNonEmptyResultsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPActionSearchValidActionSearchRequestWithNonEmptyResultsTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"resource": { "type": "record", "id": "record-1" }
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
