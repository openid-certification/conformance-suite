package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-reject-get-method",
	displayName = "AuthZEN Evaluation API - Section 10.1: Reject GET method",
	summary = "Per Section 10.1, evaluation requests are made via HTTPS POST. The PDP is expected to reject a GET request to the evaluation endpoint with HTTP 400, 404 or 405 (typically 405 Method Not Allowed). Section 10.1 places the POST requirement on the request rather than on the PDP, so accepting the request is reported as a warning.",
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationRejectGetMethodTest extends AbstractAuthzenPDPEvaluationTest {

	// The body is irrelevant for a GET: the PDP is expected to refuse the verb before parsing.
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

	@Override
	protected ConditionResult getUnexpectedHttpStatusSeverity() {
		return ConditionResult.WARNING;
	}
}
