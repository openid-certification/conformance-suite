package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationIdempotencyTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-idempotency",
	displayName = "Authzen Evaluation API - Idempotency",
	summary = "Idempotency. The harness sends the same fixture request multiple times consecutively; the PDP MUST return the same decision value each time.\n" + AuthzenPDPEvaluationIdempotencyTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationIdempotencyTest extends AbstractAuthzenPDPEvaluationIdempotencyTest {

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
	protected String getExpectedDecisionResponseJson() {
		return """
			{ "decision": true }
			""";
	}
}
