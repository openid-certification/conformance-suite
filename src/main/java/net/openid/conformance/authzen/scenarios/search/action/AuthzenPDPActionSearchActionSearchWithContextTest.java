package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-action-search-with-context",
	displayName = "Authzen Action Search API - Section 8.6.1: Action search with context",
	summary = "Section 8.6.1 action search with the optional context field. The PDP MUST accept the request and return at least read and write.\n" + AuthzenPDPActionSearchActionSearchWithContextTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPActionSearchActionSearchWithContextTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
					{ "name": "read" },
					{ "name": "write" }
				]
			}
			""";
	}
}
