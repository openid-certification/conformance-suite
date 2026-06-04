package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-x-request-id-echoed",
	displayName = "Authzen Evaluations API - X-Request-ID echoed",
	summary = "Section 10.1.3 — when the PEP supplies an X-Request-ID on an Access Evaluations request, the PDP MUST return the same value in the response.\n" + AuthzenPDPEvaluationsXRequestIdEchoedTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsXRequestIdEchoedTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"evaluations": [
				{ "resource": { "type": "record", "id": "record-1" } },
				{ "resource": { "type": "record", "id": "record-2" } }
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
					{ "decision": true }
				]
			}
			""";
	}
}
