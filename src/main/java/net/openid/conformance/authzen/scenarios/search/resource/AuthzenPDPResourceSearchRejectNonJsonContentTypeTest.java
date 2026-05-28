package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-reject-non-json-content-type",
	displayName = "Authzen Resource Search API - Spec 10.1-2: Reject non-JSON Content-Type",
	summary = "Per spec 10.1-2, requests MUST include `Content-Type: application/json`. The PDP MUST reject a request with `Content-Type: text/plain` with HTTP 4xx (typically 415 Unsupported Media Type, sometimes 400).",
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchRejectNonJsonContentTypeTest extends AbstractAuthzenPDPResourceSearchTest {

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
		return "{}";
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getRequestContentTypeOverride() {
		return "text/plain";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 415;
	}
}
