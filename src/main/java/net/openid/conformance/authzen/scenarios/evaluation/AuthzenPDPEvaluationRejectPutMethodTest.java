package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-reject-put-method",
	displayName = "Authzen Evaluation API - Section 10.1: Reject PUT method",
	summary = "Per Section 10.1, evaluation requests are made via HTTPS POST. The PDP MUST reject a PUT request to the evaluation endpoint with an HTTP 4xx error (typically 405 Method Not Allowed).",
	profile = "Authzen"
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
