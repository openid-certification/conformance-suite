package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-default-semantic-continues-after-deny",
	displayName = "AuthZEN Evaluations API - Section 7.1.2.1: default semantic continues after a deny",
	summary = "Section 7.1.2.1: \"By default, every request in the evaluations array is executed and a response returned in the same array order\", and execute_all is the semantic of a request without `options.evaluations_semantic`. "
		+ "This test sends no `options` and puts the deny first: bob writes record-1 (rule 4 = false), then bob reads record-1 (rule 3 = true). "
		+ "The PDP MUST return HTTP 200 with both decisions in request order. A PDP that stops at the first deny returns only the first decision and fails.\n" + AuthzenPDPEvaluationsDefaultSemanticContinuesAfterDenyTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsDefaultSemanticContinuesAfterDenyTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "bob" },
			"resource": { "type": "record", "id": "record-1" },
			"evaluations": [
				{ "action": { "name": "write" } },
				{ "action": { "name": "read" } }
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
					{ "decision": false },
					{ "decision": true }
				]
			}
			""";
	}

	@Override
	protected String[] additionalDecisionsRequirements() {
		// Section 7.1.2.1: "By default, every request in the evaluations array is executed and a
		// response returned in the same array order".
		return new String[] {"AUTHZEN-7.1.2.1"};
	}
}
