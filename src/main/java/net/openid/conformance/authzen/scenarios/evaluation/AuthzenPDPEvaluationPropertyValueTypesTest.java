package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-property-value-types-accepted",
	displayName = "AuthZEN Evaluation API - Section 5.1.1: Property values of every JSON type",
	summary = "Per Section 5.1.1, \"A property can contain both simple values, such as strings, numbers, booleans and nulls, and complex values, such as arrays and objects.\" The subject alice carries a string, a number, a boolean, an array and an object property, under names no fixture rule uses, on the fixture request alice reads record-1 (rule 1). No null is sent, as Section 11.5 says properties with a value of null SHOULD be omitted. The PDP MUST accept the request and return HTTP 200 with `{ \"decision\": true }`.\n" + AuthzenPDPEvaluationPropertyValueTypesTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationPropertyValueTypesTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "alice",
				"properties": {
					"text": "value",
					"count": 1,
					"enabled": true,
					"list": [1, "two"],
					"nested": { "key": "value" }
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
		// Section 5.1.1: a property may hold simple and complex values.
		return new String[] {"AUTHZEN-5.1.1"};
	}
}
