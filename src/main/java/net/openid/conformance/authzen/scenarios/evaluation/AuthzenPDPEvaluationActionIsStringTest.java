package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-action-is-string",
	displayName = "AuthZEN Evaluation API - Section 5.3: Action is string -- expect HTTP 400",
	summary = "Section 5.3 invalid field type (certification scenario 5.4.6). `action` is the string \"read\" instead of an object; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationActionIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationActionIsStringTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": "read",
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 5.3: "Action is an object".
		return new String[] {"AUTHZEN-5.3"};
	}
}
