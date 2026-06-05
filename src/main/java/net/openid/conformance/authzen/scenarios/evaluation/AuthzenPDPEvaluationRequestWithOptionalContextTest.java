package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-request-with-optional-context",
	displayName = "Authzen Evaluation API - Section 6.1: Request with optional context",
	summary = "Section 6.1 request with optional context. The fixture decision MUST NOT change when context is supplied; expects {\"decision\": true}.\n" + AuthzenPDPEvaluationRequestWithOptionalContextTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationRequestWithOptionalContextTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"context": {
				"time": "2025-06-27T18:03-07:00",
				"ip": "192.168.1.1"
			}
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
