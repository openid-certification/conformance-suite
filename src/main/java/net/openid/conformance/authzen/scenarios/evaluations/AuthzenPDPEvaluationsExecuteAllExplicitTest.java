package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-execute-all-explicit",
	displayName = "AuthZEN Evaluations API - Section 7.1.2.1: evaluations_semantic explicitly set to execute_all",
	summary = "Per Section 7.1.2.1, `evaluations_semantic: \"execute_all\"` MUST be accepted and MUST process every evaluation regardless of earlier decisions. Sends the mixed permit/deny batch of authzen-pdp-evaluations-batch-with-fixture-decisions-validated, which relies on execute_all being the default, with the option set explicitly instead, and asserts both decisions come back in request order.\n" + AuthzenPDPEvaluationsExecuteAllExplicitTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsExecuteAllExplicitTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "read" } },
				{ "action": { "name": "write" } }
			],
			"options": { "evaluations_semantic": "execute_all" }
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
