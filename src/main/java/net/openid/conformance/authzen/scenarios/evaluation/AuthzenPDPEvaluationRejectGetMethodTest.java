package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-reject-get-method",
	displayName = "Authzen Evaluation API - Spec 10.1-1: Reject GET method",
	summary = "Per spec 10.1-1, evaluation requests are made via HTTPS POST. The PDP MUST reject a GET request to the evaluation endpoint with an HTTP 4xx error (typically 405 Method Not Allowed).",
	profile = "Authzen"
)
public class AuthzenPDPEvaluationRejectGetMethodTest extends AbstractAuthzenPDPEvaluationTest {

	// Body is irrelevant for a GET — the PDP MUST refuse the verb before parsing.
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
		return "GET";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 405;
	}
}
