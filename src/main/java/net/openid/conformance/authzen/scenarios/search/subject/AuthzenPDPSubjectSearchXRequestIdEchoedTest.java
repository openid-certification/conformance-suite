package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-x-request-id-echoed",
	displayName = "Authzen Subject Search API - X-Request-ID echoed",
	summary = "Spec 10.1.3-4 — when the PEP supplies an X-Request-ID on a Subject Search request, the PDP MUST return the same value in the response.\n" + AuthzenPDPSubjectSearchXRequestIdEchoedTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchXRequestIdEchoedTest extends AbstractAuthzenPDPSubjectSearchTest {

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
	protected boolean includeXRequestIdHeader() {
		return true;
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
