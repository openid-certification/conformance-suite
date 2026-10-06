package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-with-context-inheritance",
	displayName = "AuthZEN Evaluations API - Section 7.1: Batch with context inheritance",
	summary = "Section 7.1 batch with context inheritance (certification scenario 6.2.6). Top-level context applies to all evaluations unless overridden per-evaluation, and the top-level subject and action apply to both evaluations, which name only their resource. The fixture decisions do not depend on context (scenario 4.6): alice may read record-1 (rule 1), so the first decision MUST be `true`. The fixture defines no decision for alice reading record-2, so the second decision may be either value.\n" + AuthzenPDPEvaluationsBatchWithContextInheritanceTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsBatchWithContextInheritanceTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"context": {
				"time": "2025-06-27T18:03-07:00"
			},
			"evaluations": [
				{
					"resource": { "type": "record", "id": "record-1" }
				},
				{
					"resource": { "type": "record", "id": "record-2" },
					"context": {
						"time": "2025-06-27T19:00-07:00",
						"source": "batch-override"
					}
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
					{ }
				]
			}
			""";
	}
}
