package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-request-with-evaluations-array",
	displayName = "Authzen Evaluations API - Section 7.1: Batch request with evaluations array",
	summary = "Section 7.1 batch request with `evaluations` array. The PDP MUST return a two-element response array; both decisions are strictly compared by index — alice/read on record-1 and record-2 are both permitted in the fixture, so both decisions MUST be `true`.\n" + AuthzenPDPEvaluationsBatchRequestWithEvaluationsArrayTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsBatchRequestWithEvaluationsArrayTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"evaluations": [
				{ "resource": { "type": "record", "id": "record-1" } },
				{ "resource": { "type": "record", "id": "record-2" } }
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
					{ "decision": true }
				]
			}
			""";
	}
}
