package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-unknown-entity-type",
	displayName = "Authzen Subject Search API - Section 8.3: Unknown entity type",
	summary = "Section 8.3 unknown entity type. The PDP MUST return an empty results array (not HTTP 400) when the subject type is unrecognised.\n" + AuthzenPDPSubjectSearchUnknownEntityTypeTest.payload,
	profile = "Authzen"
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
