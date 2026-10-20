package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-unknown-evaluation-fields-ignored",
	displayName = "AuthZEN Evaluations API - Section 10.1.1: Unknown fields in an evaluation ignored",
	summary = "Per Section 10.1.1, \"receivers MUST ignore unknown fields present in request or response bodies\"; the suite applies this to members nested in the body as well as at its top level. Each item of a fixture batch request (bob read then write record-1) carries `foo` and `futureField`, which no specification defines; PDP MUST return HTTP 200 with the correct decisions ([true, false]).\n" + AuthzenPDPEvaluationsUnknownEvaluationFieldsTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsUnknownEvaluationFieldsTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "read" }, "foo": "bar", "futureField": { "nested": true } },
				{ "action": { "name": "write" }, "foo": "bar", "futureField": { "nested": true } }
			]
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

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 10.1.1: "receivers MUST ignore unknown fields present in request or response bodies".
		return new String[] {"AUTHZEN-10.1.1"};
	}
}
