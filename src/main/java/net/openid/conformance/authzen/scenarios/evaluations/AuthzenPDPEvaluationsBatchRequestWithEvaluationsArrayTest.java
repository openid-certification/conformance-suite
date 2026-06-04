package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-request-with-evaluations-array",
	displayName = "Authzen Evaluations API - Section 7.1: Batch request with evaluations array",
	summary = "Section 7.1 batch request with evaluations array. Structural test only: a response array of two decision objects is expected (decision values are not validated).\n" + AuthzenPDPEvaluationsBatchRequestWithEvaluationsArrayTest.payload,
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
