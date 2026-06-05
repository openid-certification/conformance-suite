package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-unknown-semantic-value",
	displayName = "Authzen Evaluations API - Section 7.1.2.1: Unknown evaluations_semantic value returns 400",
	summary = "Per Section 7.1.2.1, `evaluations_semantic` only accepts `execute_all`, `deny_on_first_deny`, and `permit_on_first_permit`. A request with an unrecognized value MUST be rejected with HTTP 400.",
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsUnknownSemanticValueTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"evaluations": [
				{ "resource": { "type": "record", "id": "record-1" } }
			],
			"options": { "evaluations_semantic": "bogus_value" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedEvaluationsResponseJson() {
		return "{}";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}
}
