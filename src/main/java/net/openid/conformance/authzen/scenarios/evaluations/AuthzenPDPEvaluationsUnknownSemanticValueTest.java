package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-unknown-semantic-value",
	displayName = "AuthZEN Evaluations API - Section 7.1.2.1: Unknown evaluations_semantic value",
	summary = "Section 7.1.2.1 lists the three values a PEP can send in `evaluations_semantic`: `execute_all`, `deny_on_first_deny` and `permit_on_first_permit`. It does not say how a PDP answers any other value. "
		+ "This test sends an unrecognized value and expects HTTP 400, as for any invalid request. It is only in the comprehensive plan, not in certification, because a PDP that does not implement the evaluation semantics may ignore `options`.",
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
