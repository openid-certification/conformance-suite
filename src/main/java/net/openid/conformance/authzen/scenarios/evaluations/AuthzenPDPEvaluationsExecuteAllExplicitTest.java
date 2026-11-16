package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-execute-all-explicit",
	displayName = "AuthZEN Evaluations API - Section 7.1.2.1: evaluations_semantic explicitly set to execute_all",
	summary = "Section 7.1.2.1 lists `execute_all` among the values a PEP can pass in `options.evaluations_semantic`, and defines it as executing all of the requests and returning all of the results, in the same array order; it is also the default. Certification scenario 6.4.1 sends the option explicitly in the same way. This test sends `evaluations_semantic: \"execute_all\"` with the deny first: bob writes record-1 (rule 4 = false), then bob reads record-1 (rule 3 = true). The PDP MUST return HTTP 200 with both decisions in request order; a PDP that stops at the deny returns only the first decision and fails.\n" + AuthzenPDPEvaluationsExecuteAllExplicitTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsExecuteAllExplicitTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "write" } },
				{ "action": { "name": "read" } }
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
					{ "decision": false },
					{ "decision": true }
				]
			}
			""";
	}

	@Override
	protected String[] additionalDecisionsRequirements() {
		// Section 7.1.2.1: execute_all is "Execute all of the requests (potentially in parallel),
		// return all of the results".
		return new String[] {"AUTHZEN-7.1.2.1"};
	}
}
