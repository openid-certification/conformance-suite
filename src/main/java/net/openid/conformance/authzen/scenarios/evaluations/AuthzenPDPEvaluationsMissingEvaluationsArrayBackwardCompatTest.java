package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsBackwardCompatTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-missing-evaluations-array-backward-compat",
	displayName = "Authzen Evaluations API - Section 7.1: Backward compat with missing `evaluations` array",
	summary = "Per Section 7.1, a request to the Evaluations endpoint that omits the `evaluations` array MUST be handled like the single Access Evaluation API: the PDP returns a single-decision response (`{decision: <bool>}`) and MUST NOT return an `evaluations` array. Sends a single-evaluation-shaped request (alice/read/record-1, fixture rule 1) and validates the response shape and that the decision is `true`.\n" + AuthzenPDPEvaluationsMissingEvaluationsArrayBackwardCompatTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsMissingEvaluationsArrayBackwardCompatTest extends AbstractAuthzenPDPEvaluationsBackwardCompatTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}
}
