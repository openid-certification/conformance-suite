package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-permit-on-first-permit",
	displayName = "Authzen Evaluations API - Section 7.1.2.1: permit_on_first_permit semantic",
	summary = "Per Section 7.1.2.1, with `evaluations_semantic: \"permit_on_first_permit\"` the PDP stops further processing once a `true` decision is reached. This test profile requires the short-circuit to be observable on the wire: the response `evaluations` array MUST be truncated at the first `true` (length = trigger_position + 1). Evaluation 1 denies naturally (bob/write); evaluation 2 permits (bob/read, the trigger); evaluation 3 must not be returned.\n" + AuthzenPDPEvaluationsPermitOnFirstPermitTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsPermitOnFirstPermitTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "write" } },
				{ "action": { "name": "read" } },
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
					{ "decision": true }
				]
			}
			""";
	}
}
