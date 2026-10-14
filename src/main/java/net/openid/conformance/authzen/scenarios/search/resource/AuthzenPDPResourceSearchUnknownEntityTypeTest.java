package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-unknown-entity-type",
	displayName = "AuthZEN Resource Search API - Certification scenario 7.6.2: Unknown entity type",
	summary = "Certification scenario 7.6.2 unknown entity type. The searched-for resource type does not exist in the fixture, so the PDP MUST return an empty results array, not HTTP 400. Scenario 4.6 lets a PDP have policies beyond the fixture, and one that matches the unknown entity would make the results non-empty; whether 7.6 overrides that is openid/authzen#765.\n" + AuthzenPDPResourceSearchUnknownEntityTypeTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchUnknownEntityTypeTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "spaceship" }
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
				"results": []
			}
			""";
	}
}
