package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-x-request-id-echoed",
	displayName = "Authzen Evaluation API - Section 5.5: X-Request-ID echoed",
	summary = "Section 5.5 / Spec 10.1.3-4 — when the PEP supplies an X-Request-ID, the PDP MUST return the same value in the response.\n" + AuthzenPDPEvaluationXRequestIdEchoedTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationXRequestIdEchoedTest extends AbstractAuthzenPDPEvaluationTest {

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
	protected boolean includeXRequestIdHeader() {
		return true;
	}

	@Override
	protected String getExpectedDecisionResponseJson() {
		return """
			{ "decision": true }
			""";
	}
}
