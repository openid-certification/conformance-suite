package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchIdempotencyTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-idempotency",
	displayName = "AuthZEN Resource Search API - Idempotency",
	summary = "Idempotency. The harness sends the same Resource Search fixture request multiple times consecutively and expects the same results each time. Certification scenario 5.6 requires this of decisions only, and Section 8.1 lets search results depend on variables such as time, so a difference is reported as a warning. Each request follows every page of the response, and the results of all pages are compared in any order; how they are split into pages, `page` fields and `context` are not compared.\n" + AuthzenPDPResourceSearchIdempotencyTest.payload,
	profile = "AuthZEN"
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
