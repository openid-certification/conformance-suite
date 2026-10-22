package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-denied-action-excluded",
	displayName = "AuthZEN Action Search API - Section 8.1: An action the fixture denies is not returned",
	summary = "Section 8.1: a search result \"when subsequently used in an Access Evaluation API call, SHOULD result in a \"decision\": true response\". "
		+ "Searches for the actions bob can perform on record-1. The fixture denies bob writing record-1 (rule 4), so write in the results is reported as a warning. "
		+ "The fixture requires no action for this search, so the PDP MUST answer HTTP 200 with a results array, which may be empty; other actions are not reported (certification scenario 4.5).\n" + AuthzenPDPActionSearchDeniedActionExcludedTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchDeniedActionExcludedTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
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
			[ { "name": "write" } ]
			""";
	}
}
