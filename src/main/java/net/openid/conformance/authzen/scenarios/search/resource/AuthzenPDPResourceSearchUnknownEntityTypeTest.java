package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-unknown-entity-type",
	displayName = "AuthZEN Resource Search API - Certification scenario 7.6.2: Unknown entity type",
	summary = "Certification scenario 7.6.2 unknown entity type. The searched-for resource type does not exist in the fixture, so the PDP MUST return HTTP 200 with a results array, not HTTP 400. An empty array is what a PDP whose policies cover only the fixture returns; a non-empty one also passes, since scenario 4.6 lets a PDP have policies beyond the fixture, and one may match an identifier or type the fixture does not define. The AuthZEN working group settled this in openid/authzen#765, and the revised scenario 7.6 says the same; the harness checks only that the response is a well-formed search response.\n" + AuthzenPDPResourceSearchUnknownEntityTypeTest.payload,
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
