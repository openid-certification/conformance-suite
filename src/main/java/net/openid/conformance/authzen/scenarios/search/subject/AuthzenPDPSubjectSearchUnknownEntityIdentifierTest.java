package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-unknown-entity-identifier",
	displayName = "AuthZEN Subject Search API - Certification scenario 7.6.1: Unknown entity identifier",
	summary = "Certification scenario 7.6.1 unknown entity identifier. The resource identifier does not exist in the fixture, so the PDP MUST return HTTP 200 with a results array, not an error. An empty array is what a PDP whose policies cover only the fixture returns; a non-empty one also passes, since scenario 4.6 lets a PDP have policies beyond the fixture, and one may match an identifier or type the fixture does not define. The AuthZEN working group settled this in openid/authzen#765, and the revised scenario 7.6 says the same; the harness checks only that the response is a well-formed search response.\n" + AuthzenPDPSubjectSearchUnknownEntityIdentifierTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchUnknownEntityIdentifierTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "nonexistent-record" }
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
