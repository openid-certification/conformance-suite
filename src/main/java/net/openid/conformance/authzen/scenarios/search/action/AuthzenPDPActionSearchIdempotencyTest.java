package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchIdempotencyTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-idempotency",
	displayName = "AuthZEN Action Search API - Idempotency",
	summary = "Idempotency. The harness sends the same Action Search fixture request multiple times consecutively and expects the same results each time. Certification scenario 5.6 requires this of decisions only, and Section 8.1 lets search results depend on variables such as time, so a difference is reported as a warning. Each request follows every page of the response, and the results of all pages are compared in any order; each result is compared without its `properties`, which describe the entity rather than identify it; how the results are split into pages, `page` fields and `context` are not compared.\n" + AuthzenPDPActionSearchIdempotencyTest.payload,
	profile = "AuthZEN"
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
