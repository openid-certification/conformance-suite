package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsBackwardCompatTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-missing-evaluations-array-backward-compat",
	displayName = "Authzen Evaluations API - Section 7.1: Backward compat with missing `evaluations` array",
	summary = "Per Section 7.1, a request to the Evaluations endpoint that omits the `evaluations` array MAY receive either the single-decision form (`{decision: <bool>}`) or the one-element evaluations array form (`{evaluations: [{decision: <bool>}]}`). Sends a single-evaluation-shaped request (alice/read/record-1) and accepts either response shape.\n" + AuthzenPDPEvaluationsMissingEvaluationsArrayBackwardCompatTest.payload,
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

	@Override
	protected String getExpectedEvaluationsResponseJson() {
		return """
			{
				"evaluations": [
					{ "decision": true }
				]
			}
			""";
	}
}
