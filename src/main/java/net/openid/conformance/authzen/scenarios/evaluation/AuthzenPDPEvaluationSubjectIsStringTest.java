package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-subject-is-string",
	displayName = "AuthZEN Evaluation API - Section 5.1: Subject is string -- expect HTTP 400",
	summary = "Section 5.1 invalid field type (certification scenario 5.4.6). `subject` is a string instead of an object; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationSubjectIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationSubjectIsStringTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": "alice",
			"action": { "name": "read" },
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
		// Section 5.1: "A Subject is an object".
		return new String[] {"AUTHZEN-5.1"};
	}
}
