package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-deny-on-first-deny-without-denial",
	displayName = "AuthZEN Evaluations API - Section 7.1.2.1: deny_on_first_deny with no denial",
	summary = "Per Section 7.1.2.1, `deny_on_first_deny` short-circuits on the first denial and \"works like the && operator\", so a batch in which no evaluation is denied is evaluated in full. Every evaluation here is permitted by a fixture rule: bob reading record-1 (rule 3), alice reading record-1 (rule 1) and alice writing record-1 (rule 2). The response MUST carry all three decisions, `true`, `true`, `true`; a PDP that drops results without reaching a denial fails.\n" + AuthzenPDPEvaluationsDenyOnFirstDenyWithoutDenialTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsDenyOnFirstDenyWithoutDenialTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "subject": { "type": "user", "id": "bob" }, "action": { "name": "read" } },
				{ "subject": { "type": "user", "id": "alice" }, "action": { "name": "read" } },
				{ "subject": { "type": "user", "id": "alice" }, "action": { "name": "write" } }
			],
			"options": { "evaluations_semantic": "deny_on_first_deny" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedEvaluationsResponseJson() {
		return """
			{
				"evaluations": [
					{ "decision": true },
					{ "decision": true },
					{ "decision": true }
				]
			}
			""";
	}
}
