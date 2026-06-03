package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-with-fully-specified-evaluations",
	displayName = "Authzen Evaluations API - Section 6.1: Batch with fully specified evaluations (no defaults)",
	summary = "Section 6.1 batch with fully specified evaluations. Each item supplies subject, action, and resource; expects [true, false].\n" + AuthzenPDPEvaluationsBatchWithFullySpecifiedEvaluationsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsBatchWithFullySpecifiedEvaluationsTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"evaluations": [
				{
					"subject": { "type": "user", "id": "alice" },
					"action": { "name": "read" },
					"resource": { "type": "record", "id": "record-1" }
				},
				{
					"subject": { "type": "user", "id": "bob" },
					"action": { "name": "write" },
					"resource": { "type": "record", "id": "record-1" }
				}
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
