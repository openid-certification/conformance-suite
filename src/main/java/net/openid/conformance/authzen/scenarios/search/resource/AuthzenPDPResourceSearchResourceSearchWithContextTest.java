package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-resource-search-with-context",
	displayName = "AuthZEN Resource Search API - Section 8.5.1: Resource search with context",
	summary = "Section 8.5.1 resource search with the optional context field (certification scenario 7.3.2). The PDP MUST accept the request, and as context does not affect fixture decisions (scenario 4.6), the results MUST include record-1.\n" + AuthzenPDPResourceSearchResourceSearchWithContextTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchResourceSearchWithContextTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" },
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
					{ "type": "record", "id": "record-1" }
				]
			}
			""";
	}
}
