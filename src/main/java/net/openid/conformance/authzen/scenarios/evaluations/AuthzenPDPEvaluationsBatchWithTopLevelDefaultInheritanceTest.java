package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-batch-with-top-level-default-inheritance",
	displayName = "AuthZEN Evaluations API - Section 7.1.1: Batch with top-level default inheritance",
	summary = "Section 7.1.1 batch with default values (certification scenario 6.2.7). The first evaluation is empty, so it inherits the whole top-level subject, action and resource: alice writing the active record-1, which is permitted. The second evaluation supplies a complete resource, which replaces the top-level resource as a whole: alice writing the archived record-2, which is denied. Expects [true, false].\n" + AuthzenPDPEvaluationsBatchWithTopLevelDefaultInheritanceTest.payload,
	profile = "AuthZEN"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPEvaluationsBatchWithTopLevelDefaultInheritanceTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "write" },
			"resource": { "type": "record", "id": "record-1", "properties": { "status": "active" } },
			"evaluations": [
				{},
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
