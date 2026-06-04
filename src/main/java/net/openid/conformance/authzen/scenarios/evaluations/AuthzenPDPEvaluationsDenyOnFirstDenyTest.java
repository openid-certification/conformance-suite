package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-deny-on-first-deny",
	displayName = "Authzen Evaluations API - Section 7.1.2.1: deny_on_first_deny semantic",
	summary = "Per Section 7.1.2.1, with `evaluations_semantic: \"deny_on_first_deny\"` the PDP MAY stop further processing once a `false` decision is reached and either truncate the response after that point or return `false` for every subsequent evaluation (see the example in 7.1.2.1.1). Evaluation 1 permits naturally (bob/read), evaluation 2 denies (bob/write, the trigger), evaluation 3 would naturally permit but MUST be short-circuited to `false`.\n" + AuthzenPDPEvaluationsDenyOnFirstDenyTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsDenyOnFirstDenyTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "read" } },
				{ "action": { "name": "write" } },
				{ "action": { "name": "read" } }
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
					{ "decision": false },
					{ "decision": false }
				]
			}
			""";
	}
}
