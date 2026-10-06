package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-request-with-evaluations-array",
	displayName = "AuthZEN Evaluations API - Section 7.1: Batch request with evaluations array",
	summary = "Section 7.1 batch request with `evaluations` array (certification scenario 6.2.1). The subject and action are given once at the top level and each evaluation names only its resource, so the PDP MUST apply the top-level values to both (Section 7.1.1) and return a two-element response array. Alice may read record-1 (rule 1), so the first decision MUST be `true`. The fixture defines no decision for alice reading record-2, so the second decision may be either value.\n" + AuthzenPDPEvaluationsBatchRequestWithEvaluationsArrayTest.payload,
	profile = "AuthZEN"
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
					{ }
				]
			}
			""";
	}
}
