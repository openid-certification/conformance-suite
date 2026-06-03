package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-request-with-additional-properties",
	displayName = "Authzen Evaluation API - Section 6.1: Request with additional properties",
	summary = "Section 6.1 request with additional properties beyond those required by the fixture; the PDP MUST accept and return {\"decision\": true}.\n" + AuthzenPDPEvaluationRequestWithAdditionalPropertiesTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationRequestWithAdditionalPropertiesTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "alice",
				"properties": {
					"department": "Sales",
					"role": "manager"
				}
			},
			"action": {
				"name": "read",
				"properties": {
					"method": "GET"
				}
			},
			"resource": {
				"type": "record",
				"id": "record-1",
				"properties": {
					"status": "active",
					"owner": "bob"
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
