package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-fixture-request-permit-based-on-subject-properties",
	displayName = "Authzen Evaluation API - Section 6.1: Fixture request -- permit based on subject properties",
	summary = "Section 6.1 fixture request -- permit based on subject properties (rule 6). An admin can write to an archived record; expects {\"decision\": true}.\n" + AuthzenPDPEvaluationFixtureRequestPermitBasedOnSubjectPropertiesTest.payload,
	profile = "Authzen"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPEvaluationFixtureRequestPermitBasedOnSubjectPropertiesTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "bob",
				"properties": {
					"role": "admin"
				}
			},
			"action": { "name": "write" },
			"resource": {
				"type": "record",
				"id": "record-2",
				"properties": {
					"status": "archived"
				}
			}
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
