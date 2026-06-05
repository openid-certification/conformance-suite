package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-fixture-request-deny-decision",
	displayName = "Authzen Evaluation API - Section 6.1: Fixture request -- deny decision",
	summary = "Section 6.1 fixture request -- deny decision. Sends bob/write/record-1 and expects {\"decision\": false}.\n" + AuthzenPDPEvaluationFixtureRequestDenyDecisionTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationFixtureRequestDenyDecisionTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"action": { "name": "write" },
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
			{ "decision": false }
			""";
	}
}
