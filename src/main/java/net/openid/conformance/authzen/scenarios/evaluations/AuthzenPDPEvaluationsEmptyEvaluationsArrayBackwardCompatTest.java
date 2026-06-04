package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsBackwardCompatTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-empty-evaluations-array-backward-compat",
	displayName = "Authzen Evaluations API - Section 7.1: Backward compat with empty `evaluations` array",
	summary = "Per spec 7.1-2, a request to the Evaluations endpoint with an empty `evaluations: []` array MAY receive either the single-decision form (`{decision: <bool>}`) or the one-element evaluations array form (`{evaluations: [{decision: <bool>}]}`). Sends a single-evaluation-shaped request with an empty evaluations array (alice/read/record-1) and accepts either response shape.\n" + AuthzenPDPEvaluationsEmptyEvaluationsArrayBackwardCompatTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsEmptyEvaluationsArrayBackwardCompatTest extends AbstractAuthzenPDPEvaluationsBackwardCompatTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": []
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
