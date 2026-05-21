package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-fixture-request-deny-based-on-action-properties",
	displayName = "Authzen Evaluation API - Section 2.2.7: Fixture request -- deny based on action properties",
	summary = "Section 2.2.7 fixture request -- deny based on action properties (rule 8). Hard delete is denied; expects {\"decision\": false}.\n" + AuthzenPDPEvaluationFixtureRequestDenyBasedOnActionPropertiesTest.payload,
	profile = "Authzen"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPEvaluationFixtureRequestDenyBasedOnActionPropertiesTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": {
				"name": "delete",
				"properties": {
					"soft": false
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
			{ "decision": false }
			""";
	}
}
