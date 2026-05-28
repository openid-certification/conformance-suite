package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-default-semantic-is-execute-all",
	displayName = "Authzen Evaluations API - Spec 7.1.2.1: Default evaluations_semantic is execute_all",
	summary = "Per spec 7.1.2.1, when the request omits `options.evaluations_semantic` the PDP MUST process every evaluation and return a decision for each one (`execute_all` semantic). Sends a mixed permit/deny batch and asserts both decisions are returned in order.\n" + AuthzenPDPEvaluationsDefaultSemanticIsExecuteAllTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsDefaultSemanticIsExecuteAllTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "read" } },
				{ "action": { "name": "write" } }
			]
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
					{ "decision": false }
				]
			}
			""";
	}
}
