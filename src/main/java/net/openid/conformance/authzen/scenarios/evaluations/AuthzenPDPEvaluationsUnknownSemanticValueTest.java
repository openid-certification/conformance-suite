package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-unknown-semantic-value",
	displayName = "AuthZEN Evaluations API - Section 7.1.2.1: Unknown evaluations_semantic value returns 400",
	summary = "Per Section 7.1.2.1, `evaluations_semantic` only accepts `execute_all`, `deny_on_first_deny`, and `permit_on_first_permit`. A request with an unrecognized value MUST be rejected with HTTP 400.",
	profile = "AuthZEN"
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

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 7.1.2.1 lists the values evaluations_semantic may take.
		return new String[] {"AUTHZEN-7.1.2.1"};
	}

	/**
	 * The unknown value is a string, as Section 7.1.2.1 requires, so the request breaks no
	 * Section 10.1.1 serialization rule; only the 400 response of Section 10.1.2 is cited.
	 */
	@Override
	protected String[] badRequestRequirements() {
		return new String[] {"AUTHZEN-10.1.2"};
	}
}
