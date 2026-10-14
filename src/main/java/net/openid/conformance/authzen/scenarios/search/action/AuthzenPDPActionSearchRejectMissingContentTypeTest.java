package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-reject-missing-content-type",
	displayName = "AuthZEN Action Search API - Section 10.1: Reject request without Content-Type",
	summary = "Per Section 10.1, requests MUST include `Content-Type: application/json`. The PDP MUST reject a request that has no Content-Type header with HTTP 400 or 415 (certification scenario 5.4.3: the Content-Type is not application/json).",
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchRejectMissingContentTypeTest extends AbstractAuthzenPDPActionSearchTest {

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
		return "{}";
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getRequestContentTypeOverride() {
		// An empty override sends the request without a Content-Type header
		return "";
	}

	@Override
	protected java.util.Set<Integer> getAcceptableHttpStatusCodes() {
		return java.util.Set.of(400, 415);
	}
}
