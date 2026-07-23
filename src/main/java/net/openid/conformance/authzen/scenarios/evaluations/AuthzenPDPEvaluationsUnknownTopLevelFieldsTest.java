package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-unknown-top-level-fields-ignored",
	displayName = "AuthZEN Evaluations API - Section 10.1.1: Unknown top-level fields ignored",
	summary = "Per Section 10.1.1, receivers MUST ignore unknown fields in the request body. Adds `foo` and `futureField` at the top level alongside a fixture batch request (bob read then write record-1); PDP MUST return HTTP 200 with the correct decisions ([true, false]).\n" + AuthzenPDPEvaluationsUnknownTopLevelFieldsTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsUnknownTopLevelFieldsTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "read" } },
				{ "action": { "name": "write" } }
			],
			"foo": "bar",
			"futureField": { "nested": true }
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
}
