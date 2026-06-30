package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-reject-get-method",
	displayName = "AuthZEN Evaluation API - Section 10.1: Reject GET method",
	summary = "Per Section 10.1, evaluation requests are made via HTTPS POST. The PDP MUST reject a GET request to the evaluation endpoint with an HTTP 4xx error (typically 405 Method Not Allowed).",
	profile = "AuthZEN"
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
	protected java.util.Set<Integer> getAcceptableHttpStatusCodes() {
		// Accept the same 4xx set as the reject-PUT tests: a PDP whose route is
		// registered for POST only may 404 an unknown verb just as plausibly for
		// GET as for PUT, so the two negative tests stay symmetric.
		return java.util.Set.of(400, 404, 405);
	}
}
