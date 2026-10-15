package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-reject-put-method",
	displayName = "AuthZEN Action Search API - Section 10.1: Reject PUT method",
	summary = "Per Section 10.1, action search requests are made via HTTPS POST. The PDP is expected to reject a PUT request to the action search endpoint with 405 Method Not Allowed, preferably with an `Allow: POST` header (RFC 7231 Section 6.5.5), or 400. A 404 is reported as a warning, as it is only expected when the server framework routes on method and path. Section 10.1 places the POST requirement on the request rather than on the PDP, so a PDP that accepts the request is reported as a warning. Any other status, such as a 5xx, fails.",
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchRejectPutMethodTest extends AbstractAuthzenPDPActionSearchTest {

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
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getRequestHttpMethod() {
		return "PUT";
	}

	@Override
	protected java.util.Set<Integer> getAcceptableHttpStatusCodes() {
		return java.util.Set.of(400, 404, 405);
	}
}
