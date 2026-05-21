package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-with-fixture-decisions-validated",
	displayName = "Authzen Evaluations API - Section 3.2.2: Batch with fixture decisions validated",
	summary = "Section 3.2.2 batch with fixture decisions validated. Bob reads record-1 (rule 3 = true) then writes record-1 (rule 4 = false). Validates structure, decision values, and ordering.\n" + AuthzenPDPEvaluationsBatchWithFixtureDecisionsValidatedTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsBatchWithFixtureDecisionsValidatedTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "read" } },
				{ "action": { "name": "write" } }
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
