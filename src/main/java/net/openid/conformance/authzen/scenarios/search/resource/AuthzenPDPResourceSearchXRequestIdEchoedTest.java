package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-x-request-id-echoed",
	displayName = "Authzen Resource Search API - X-Request-ID echoed",
	summary = "Section 10.1.3 — when the PEP supplies an X-Request-ID on a Resource Search request, the PDP MUST return the same value in the response.\n" + AuthzenPDPResourceSearchXRequestIdEchoedTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchXRequestIdEchoedTest extends AbstractAuthzenPDPResourceSearchTest {

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
	protected boolean includeXRequestIdHeader() {
		return true;
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
