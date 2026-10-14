package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-reject-non-json-content-type",
	displayName = "AuthZEN Evaluations API - Section 10.1: Reject non-JSON Content-Type",
	summary = "Per Section 10.1, requests MUST include `Content-Type: application/json`. The PDP MUST reject a request with `Content-Type: text/plain` with HTTP 400 or 415 Unsupported Media Type. Any other status fails, including 404: an unsupported media type is a problem with the content, not a missing route.",
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsRejectNonJsonContentTypeTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"evaluations": [
				{ "resource": { "type": "record", "id": "record-1" } }
			]
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedEvaluationsResponseJson() {
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
	protected java.util.Set<Integer> getAcceptableHttpStatusCodes() {
		return java.util.Set.of(400, 415);
	}
}
