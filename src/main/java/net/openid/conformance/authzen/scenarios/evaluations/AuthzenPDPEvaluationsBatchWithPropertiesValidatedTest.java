package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-with-properties-validated",
	displayName = "Authzen Evaluations API - Section 7.1: Batch with properties validated",
	summary = "Section 7.1 batch with properties validated (rules 2 and 5). Per-evaluation resource properties drive the per-item decision; expects [true, false].\n" + AuthzenPDPEvaluationsBatchWithPropertiesValidatedTest.payload,
	profile = "Authzen"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPEvaluationsBatchWithPropertiesValidatedTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "write" },
			"evaluations": [
				{
					"resource": {
						"type": "record",
						"id": "record-1",
						"properties": { "status": "active" }
					}
				},
				{
					"resource": {
						"type": "record",
						"id": "record-2",
						"properties": { "status": "archived" }
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
					{ "decision": false }
				]
			}
			""";
	}
}
