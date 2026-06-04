package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-with-subject-properties-validated",
	displayName = "Authzen Evaluations API - Section 7.1: Batch with subject properties validated",
	summary = "Section 7.1 batch with subject properties validated (rules 5 and 6). Per-evaluation subject properties drive the per-item decision; expects [false, true].\n" + AuthzenPDPEvaluationsBatchWithSubjectPropertiesValidatedTest.payload,
	profile = "Authzen"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPEvaluationsBatchWithSubjectPropertiesValidatedTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"action": { "name": "write" },
			"resource": {
				"type": "record",
				"id": "record-2",
				"properties": { "status": "archived" }
			},
			"evaluations": [
				{
					"subject": { "type": "user", "id": "alice" }
				},
				{
					"subject": {
						"type": "user",
						"id": "bob",
						"properties": { "role": "admin" }
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
					{ "decision": false },
					{ "decision": true }
				]
			}
			""";
	}
}
