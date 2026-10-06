package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-accept-content-type-with-charset",
	displayName = "AuthZEN Evaluations API - Section 10.1: Accept Content-Type with charset",
	summary = "Section 10.1 and RFC 9110, `application/json; charset=utf-8` is a valid form of the JSON Content-Type. The PDP MUST accept it and return the expected response.",
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsAcceptContentTypeWithCharsetTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"action": { "name": "write" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "subject": { "type": "user", "id": "alice" } },
				{ "subject": { "type": "user", "id": "bob" } }
			]
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getRequestContentTypeOverride() {
		return "application/json; charset=utf-8";
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
