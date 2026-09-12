package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-with-default-value-merging",
	displayName = "AuthZEN Evaluations API - Section 7.1: Batch with default value merging",
	summary = "Section 7.1.1 batch with default values. Neither evaluation carries a subject or an action, so both inherit the top-level defaults whole; each supplies its own complete resource. Expects [true, false].\n" + AuthzenPDPEvaluationsBatchWithDefaultValueMergingTest.payload,
	profile = "AuthZEN"
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
