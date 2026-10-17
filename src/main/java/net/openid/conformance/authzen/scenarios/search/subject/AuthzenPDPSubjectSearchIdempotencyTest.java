package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchIdempotencyTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-idempotency",
	displayName = "AuthZEN Subject Search API - Idempotency",
	summary = "Idempotency. The harness sends the same Subject Search fixture request multiple times consecutively and expects the same results each time. Certification scenario 5.6 requires this of decisions only, and Section 8.1 lets search results depend on variables such as time, so a difference is reported as a warning. Each request follows every page of the response, and the results of all pages are compared in any order; each result is compared without its `properties`, which describe the entity rather than identify it; how the results are split into pages, `page` fields and `context` are not compared.\n" + AuthzenPDPSubjectSearchIdempotencyTest.payload,
	profile = "AuthZEN"
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
