package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-subject-missing-id",
	displayName = "AuthZEN Evaluation API - Section 10.1.1: Subject missing id -- expect HTTP 400",
	summary = "Sections 5.1 and 10.1.1 missing required sub-field (certification scenario 5.4.2). `subject` omits `id`; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationSubjectMissingIdTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationSubjectMissingIdTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
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
		// Section 5.1: a Subject has two REQUIRED keys, type and id.
		return new String[] {"AUTHZEN-5.1"};
	}
}
