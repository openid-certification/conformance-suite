package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-evaluation-level-errors",
	displayName = "AuthZEN Evaluations API - Section 7.1.2.1: Evaluation-level errors",
	summary = "Section 7.1.2.1 evaluation-level errors under the execute_all semantic, which the request sets explicitly (certification scenario 6.4.1). The second evaluation is empty and MUST be returned as a decision-false item in the response array (HTTP 200).\n" + AuthzenPDPEvaluationsEvaluationLevelErrorsTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsEvaluationLevelErrorsTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"options": { "evaluations_semantic": "execute_all" },
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
