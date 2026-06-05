package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-valid-subject-search-request-with-non-empty-results",
	displayName = "Authzen Subject Search API - Section 8.4.1: Valid subject search request with non-empty results",
	summary = "Section 8.4.1 valid subject search. Search for users who can read record-1; the results MUST include at least alice and bob.\n" + AuthzenPDPSubjectSearchValidSubjectSearchRequestWithNonEmptyResultsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchValidSubjectSearchRequestWithNonEmptyResultsTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
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
					{ "type": "user", "id": "alice" },
					{ "type": "user", "id": "bob" }
				]
			}
			""";
	}
}
