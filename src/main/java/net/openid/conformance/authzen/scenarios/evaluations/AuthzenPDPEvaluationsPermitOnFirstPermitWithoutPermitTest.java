package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-permit-on-first-permit-without-permit",
	displayName = "AuthZEN Evaluations API - Section 7.1.2.1: permit_on_first_permit with no permit",
	summary = "Per Section 7.1.2.1, `permit_on_first_permit` short-circuits on the first permit and works \"like the || operator\", so a batch in which no evaluation is permitted is evaluated in full. The only identifier-only fixture rule that denies is bob writing record-1 (rule 4), so the batch sends it twice. The response MUST carry both decisions, `false`, `false`; a PDP that drops results without reaching a permit fails.\n" + AuthzenPDPEvaluationsPermitOnFirstPermitWithoutPermitTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsPermitOnFirstPermitWithoutPermitTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "write" } },
				{ "action": { "name": "write" } }
			],
			"options": { "evaluations_semantic": "permit_on_first_permit" }
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
					{ "decision": false },
					{ "decision": false }
				]
			}
			""";
	}
}
