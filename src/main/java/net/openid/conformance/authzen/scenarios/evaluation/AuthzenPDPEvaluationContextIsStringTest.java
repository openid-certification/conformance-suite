package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-context-is-string",
	displayName = "AuthZEN Evaluation API - Section 5.4: Context is string -- expect HTTP 400",
	summary = "Section 5.4 invalid field type (certification scenario 5.4.6). `context` is a string instead of an object; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationContextIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationContextIsStringTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"context": "ip=192.168.1.1"
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
		// Section 5.4: "Context is an object".
		return new String[] {"AUTHZEN-5.4"};
	}
}
