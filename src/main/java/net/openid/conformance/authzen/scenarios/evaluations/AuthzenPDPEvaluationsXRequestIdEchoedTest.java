package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-x-request-id-echoed",
	displayName = "AuthZEN Evaluations API - Section 10.1.3: X-Request-ID echoed",
	summary = "Section 10.1.3 — when the PEP supplies an X-Request-ID on an Access Evaluations request, the PDP MUST return the same value in the response.\n" + AuthzenPDPEvaluationsXRequestIdEchoedTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsXRequestIdEchoedTest extends AbstractAuthzenPDPEvaluationsTest {

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
	protected boolean includeXRequestIdHeader() {
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
