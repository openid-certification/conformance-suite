package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchIdempotencyTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-idempotency",
	displayName = "Authzen Action Search API - Idempotency",
	summary = "Idempotency. The harness sends the same Action Search fixture request multiple times consecutively; the PDP MUST return the same response body each time.\n" + AuthzenPDPActionSearchIdempotencyTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPActionSearchIdempotencyTest extends AbstractAuthzenPDPActionSearchIdempotencyTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
					{ "name": "read" },
					{ "name": "write" }
				]
			}
			""";
	}
}
