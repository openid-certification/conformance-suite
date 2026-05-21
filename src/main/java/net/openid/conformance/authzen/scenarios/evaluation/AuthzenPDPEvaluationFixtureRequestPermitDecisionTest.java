package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-fixture-request-permit-decision",
	displayName = "Authzen Evaluation API - Section 2.2.1: Fixture request -- permit decision",
	summary = "Section 2.2.1 fixture request -- permit decision. Sends alice/read/record-1 and expects {\"decision\": true}.\n" + AuthzenPDPEvaluationFixtureRequestPermitDecisionTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationFixtureRequestPermitDecisionTest extends AbstractAuthzenPDPEvaluationTest {

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
