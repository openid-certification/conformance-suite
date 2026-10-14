package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-unknown-entity-identifier",
	displayName = "AuthZEN Resource Search API - Certification scenario 7.6.1: Unknown entity identifier",
	summary = "Certification scenario 7.6.1 unknown entity identifier. The subject identifier does not exist in the fixture, so the PDP MUST return an empty results array, not an error. Scenario 4.6 lets a PDP have policies beyond the fixture, and one that matches the unknown entity would make the results non-empty; whether 7.6 overrides that is openid/authzen#765.\n" + AuthzenPDPResourceSearchUnknownEntityIdentifierTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchUnknownEntityIdentifierTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "nonexistent-user" },
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
				"results": []
			}
			""";
	}
}
