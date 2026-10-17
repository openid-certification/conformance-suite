package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-subject-search-with-context",
	displayName = "AuthZEN Subject Search API - Section 8.4.1: Subject search with context",
	summary = "Section 8.4.1 subject search with the optional context field (certification scenario 7.2.2). The PDP MUST accept the request, and as context does not affect fixture decisions (scenario 4.6), the results MUST include alice and bob.\n" + AuthzenPDPSubjectSearchSubjectSearchWithContextTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchSubjectSearchWithContextTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"context": {
				"time": "2025-06-27T18:03-07:00",
				"ip": "192.168.1.1"
			}
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
