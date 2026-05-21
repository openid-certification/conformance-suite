package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-x-request-id-echoed",
	displayName = "Authzen Action Search API - X-Request-ID echoed",
	summary = "Spec 10.1.3-4 — when the PEP supplies an X-Request-ID on an Action Search request, the PDP MUST return the same value in the response.\n" + AuthzenPDPActionSearchXRequestIdEchoedTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPActionSearchXRequestIdEchoedTest extends AbstractAuthzenPDPActionSearchTest {

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
	protected boolean includeXRequestIdHeader() {
		return true;
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
