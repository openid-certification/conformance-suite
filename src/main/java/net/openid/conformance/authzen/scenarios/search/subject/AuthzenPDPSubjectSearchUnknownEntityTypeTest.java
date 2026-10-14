package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-unknown-entity-type",
	displayName = "AuthZEN Subject Search API - Certification scenario 7.6.2: Unknown entity type",
	summary = "Certification scenario 7.6.2 unknown entity type. The searched-for subject type does not exist in the fixture, so the PDP MUST return an empty results array, not HTTP 400.\n" + AuthzenPDPSubjectSearchUnknownEntityTypeTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchUnknownEntityTypeTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "spaceship" },
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
				"results": []
			}
			""";
	}
}
