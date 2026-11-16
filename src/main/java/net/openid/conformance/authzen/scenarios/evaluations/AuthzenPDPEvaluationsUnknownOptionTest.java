package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-unknown-option-ignored",
	displayName = "AuthZEN Evaluations API - Section 10.1.1: Unknown option ignored",
	summary = "Per Section 10.1.1, receivers MUST ignore unknown fields in the request body. `options` carries `another_option` beside `evaluations_semantic: execute_all`, as in the example of Section 7.1.2, on a fixture batch request (bob read then write record-1). A PDP that does not act on `options` passes as well; one that rejects an option it does not know fails. PDP MUST return HTTP 200 with the correct decisions ([true, false]).\n" + AuthzenPDPEvaluationsUnknownOptionTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsUnknownOptionTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "read" } },
				{ "action": { "name": "write" } }
			],
			"options": {
				"evaluations_semantic": "execute_all",
				"another_option": "value"
			}
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
