package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-reject-put-method",
	displayName = "Authzen Resource Search API - Section 10.1: Reject PUT method",
	summary = "Per Section 10.1, resource search requests are made via HTTPS POST. The PDP MUST reject a PUT request to the resource search endpoint with an HTTP 4xx error (typically 405 Method Not Allowed).",
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchRejectPutMethodTest extends AbstractAuthzenPDPResourceSearchTest {

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
	protected String getRequestHttpMethod() {
		return "PUT";
	}

	@Override
	protected java.util.Set<Integer> getAcceptableHttpStatusCodes() {
		return java.util.Set.of(400, 405);
	}
}
