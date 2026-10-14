package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-reject-put-method",
	displayName = "AuthZEN Evaluation API - Section 10.1: Reject PUT method",
	summary = "Per Section 10.1, evaluation requests are made via HTTPS POST. The PDP is expected to reject a PUT request to the evaluation endpoint with 405 Method Not Allowed, preferably with an `Allow: POST` header (RFC 7231 Section 6.5.5), or 400. A 404 is reported as a warning, as it is only expected when the server framework routes on method and path. Section 10.1 places the POST requirement on the request rather than on the PDP, so a PDP that accepts the request is reported as a warning. Any other status, such as a 5xx, fails.",
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationRejectPutMethodTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
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
