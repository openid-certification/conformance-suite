package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-fixture-request-permit-based-on-action-properties",
	displayName = "Authzen Evaluation API - Section 2.2.6: Fixture request -- permit based on action properties",
	summary = "Section 2.2.6 fixture request -- permit based on action properties (rule 7). Soft delete is permitted; expects {\"decision\": true}.\n" + AuthzenPDPEvaluationFixtureRequestPermitBasedOnActionPropertiesTest.payload,
	profile = "Authzen"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPEvaluationFixtureRequestPermitBasedOnActionPropertiesTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": {
				"name": "delete",
				"properties": {
					"soft": true
				}
			},
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedDecisionResponseJson() {
		return """
			{ "decision": true }
			""";
	}
}
