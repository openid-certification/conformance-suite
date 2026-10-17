package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-subject-missing-type",
	displayName = "AuthZEN Evaluation API - Section 10.1.1: Subject missing type -- expect HTTP 400",
	summary = "Sections 5.1 and 10.1.1 missing required sub-field (certification scenario 5.4.2). `subject` omits `type`; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationSubjectMissingTypeTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationSubjectMissingTypeTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "id": "alice" },
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
