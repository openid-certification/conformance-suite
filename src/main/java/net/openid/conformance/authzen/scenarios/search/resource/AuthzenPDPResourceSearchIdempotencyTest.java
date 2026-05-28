package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchIdempotencyTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-idempotency",
	displayName = "Authzen Resource Search API - Idempotency",
	summary = "Idempotency. The harness sends the same Resource Search fixture request multiple times consecutively; the PDP MUST return the same response body each time.\n" + AuthzenPDPResourceSearchIdempotencyTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchIdempotencyTest extends AbstractAuthzenPDPResourceSearchIdempotencyTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
				"results": [
					{ "type": "record", "id": "record-1" }
				]
			}
			""";
	}
}
