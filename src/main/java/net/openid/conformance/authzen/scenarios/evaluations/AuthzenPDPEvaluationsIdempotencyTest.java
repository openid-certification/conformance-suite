package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsIdempotencyTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-idempotency",
	displayName = "Authzen Evaluations API - Idempotency",
	summary = "Idempotency. The harness sends the same Evaluations fixture request multiple times consecutively; the PDP MUST return the same response body each time.\n" + AuthzenPDPEvaluationsIdempotencyTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsIdempotencyTest extends AbstractAuthzenPDPEvaluationsIdempotencyTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"evaluations": [
				{ "resource": { "type": "record", "id": "record-1" } },
				{ "resource": { "type": "record", "id": "record-2" } }
			]
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedEvaluationsResponseJson() {
		return """
			{
				"evaluations": [
					{ "decision": true },
					{ "decision": true }
				]
			}
			""";
	}
}
