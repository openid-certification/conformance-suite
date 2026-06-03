package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.AuthzenSupport;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-fixture-request-deny-based-on-properties",
	displayName = "Authzen Evaluation API - Section 6.1: Fixture request -- deny based on properties",
	summary = "Section 6.1 fixture request -- deny based on resource properties (rule 5). Alice cannot write to a record with status=archived; expects {\"decision\": false}.\n" + AuthzenPDPEvaluationFixtureRequestDenyBasedOnPropertiesTest.payload,
	profile = "Authzen"
)
@VariantNotApplicable(parameter = AuthzenSupport.class, values = {"core"})
public class AuthzenPDPEvaluationFixtureRequestDenyBasedOnPropertiesTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
			{ "decision": false }
			""";
	}
}
