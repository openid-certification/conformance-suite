package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-null-property-value-accepted",
	displayName = "AuthZEN Evaluation API - Section 11.5: Property with a null value",
	summary = "Section 11.5 says properties with a value of null SHOULD be omitted, which is a recommendation to the sender. The AuthZEN working group answered openid/authzen#779 that a receiver MUST accept a property whose value is null, and MAY treat it as absent. "
		+ "The subject alice carries one property whose value is null, under a name no fixture rule uses, on the fixture request alice reads record-1 (rule 1). The PDP MUST accept the request and return HTTP 200 with `{ \"decision\": true }`.\n" + AuthzenPDPEvaluationNullPropertyValueTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationNullPropertyValueTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "alice",
				"properties": {
					"nickname": null
				}
			},
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getExpectedDecisionResponseJson() {
		return """
			{ "decision": true }
			""";
	}

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 5.1.1: a property may hold a null; Section 11.5: null-valued properties.
		return new String[] {"AUTHZEN-5.1.1", "AUTHZEN-11.5"};
	}
}
