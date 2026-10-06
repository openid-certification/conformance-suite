package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsIdempotencyTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-idempotency",
	displayName = "AuthZEN Evaluations API - Idempotency",
	summary = "Idempotency. The harness sends the same Evaluations fixture request multiple times consecutively; the PDP MUST return the same decisions, in the same order, each time. Per-evaluation `context` is not compared.\n" + AuthzenPDPEvaluationsIdempotencyTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsIdempotencyTest extends AbstractAuthzenPDPEvaluationsIdempotencyTest {

	public static final String payload = """
		{
			"action": { "name": "write" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "subject": { "type": "user", "id": "alice" } },
				{ "subject": { "type": "user", "id": "bob" } }
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
					{ "decision": false }
				]
			}
			""";
	}
}
