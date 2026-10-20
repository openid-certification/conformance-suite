package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-member-order-not-assumed",
	displayName = "AuthZEN Evaluation API - Section 10.1.1: Members in another order",
	summary = "Per Section 10.1.1, \"Implementations MUST NOT assume a particular ordering of JSON object members.\" The fixture request alice reads record-1 (rule 1) is sent with its members in the reverse of the usual order: resource, action, then subject, and each entity's id before its type. The PDP MUST return HTTP 200 with `{ \"decision\": true }`.\n" + AuthzenPDPEvaluationMemberOrderTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationMemberOrderTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"resource": { "id": "record-1", "type": "record" },
			"action": { "name": "read" },
			"subject": { "id": "alice", "type": "user" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getExpectedDecisionResponseJson() {
		return """
			{ "decision": true }
			""";
	}

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 10.1.1: "Implementations MUST NOT assume a particular ordering of JSON object members."
		return new String[] {"AUTHZEN-10.1.1"};
	}
}
