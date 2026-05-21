package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-evaluation-level-errors",
	displayName = "Authzen Evaluations API - Section 3.4.1: Evaluation-level errors",
	summary = "Section 3.4.1 evaluation-level errors. The second evaluation is empty and MUST be returned as a decision-false item in the response array (HTTP 200).\n" + AuthzenPDPEvaluationsEvaluationLevelErrorsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsEvaluationLevelErrorsTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"evaluations": [
				{ "resource": { "type": "record", "id": "record-1" } },
				{}
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
