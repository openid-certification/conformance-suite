package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-with-default-value-merging",
	displayName = "Authzen Evaluations API - Section 3.2.7: Batch with default value merging",
	summary = "Section 3.2.7 batch with default value merging. The first item omits resource.type and inherits from the top-level default; expects [true, false].\n" + AuthzenPDPEvaluationsBatchWithDefaultValueMergingTest.payload,
	profile = "Authzen"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPEvaluationsBatchWithDefaultValueMergingTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "write" },
			"resource": { "type": "record" },
			"evaluations": [
				{
					"resource": {
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
