package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchIdempotencyTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-idempotency",
	displayName = "Authzen Subject Search API - Idempotency",
	summary = "Idempotency. The harness sends the same Subject Search fixture request multiple times consecutively; the PDP MUST return the same response body each time.\n" + AuthzenPDPSubjectSearchIdempotencyTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchIdempotencyTest extends AbstractAuthzenPDPSubjectSearchIdempotencyTest {

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
