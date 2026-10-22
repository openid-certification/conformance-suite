package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-denied-subject-excluded",
	displayName = "AuthZEN Subject Search API - Section 8.1: A subject the fixture denies is not returned",
	summary = "Section 8.1: a search result \"when subsequently used in an Access Evaluation API call, SHOULD result in a \"decision\": true response\". "
		+ "Searches for users who can write record-1. The fixture denies bob writing record-1 (rule 4), so bob in the results is reported as a warning. "
		+ "The fixture requires no user for this search, so the PDP MUST answer HTTP 200 with a results array, which may be empty; other users are not reported (certification scenario 4.5).\n" + AuthzenPDPSubjectSearchDeniedSubjectExcludedTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchDeniedSubjectExcludedTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "write" },
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getDeniedFixtureEntitiesJson() {
		return """
			[ { "type": "user", "id": "bob" } ]
			""";
	}
}
