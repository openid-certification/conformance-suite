package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-denied-resource-excluded",
	displayName = "AuthZEN Resource Search API - Section 8.1: A resource the fixture denies is not returned",
	summary = "Section 8.1: a search result \"when subsequently used in an Access Evaluation API call, SHOULD result in a \"decision\": true response\". "
		+ "Searches for records bob can write. The fixture denies bob writing record-1 (rule 4), so record-1 in the results is reported as a warning. "
		+ "The fixture requires no record for this search, so the PDP MUST answer HTTP 200 with a results array, which may be empty; other records are not reported (certification scenario 4.5).\n" + AuthzenPDPResourceSearchDeniedResourceExcludedTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchDeniedResourceExcludedTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"action": { "name": "write" },
			"resource": { "type": "record" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getDeniedFixtureEntitiesJson() {
		return """
			[ { "type": "record", "id": "record-1" } ]
			""";
	}
}
