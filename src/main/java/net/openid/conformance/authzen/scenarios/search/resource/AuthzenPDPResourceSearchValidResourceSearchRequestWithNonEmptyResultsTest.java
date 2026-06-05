package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-valid-resource-search-request-with-non-empty-results",
	displayName = "Authzen Resource Search API - Section 8.5.1: Valid resource search request with non-empty results",
	summary = "Section 8.5.1 valid resource search. Records that alice can read MUST include at least record-1.\n" + AuthzenPDPResourceSearchValidResourceSearchRequestWithNonEmptyResultsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchValidResourceSearchRequestWithNonEmptyResultsTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" }
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
