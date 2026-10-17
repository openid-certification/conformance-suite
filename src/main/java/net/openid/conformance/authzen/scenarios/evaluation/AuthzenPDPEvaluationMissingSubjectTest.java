package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-missing-subject",
	displayName = "AuthZEN Evaluation API - Section 10.1.1: Missing subject -- expect HTTP 400",
	summary = "Sections 6.1 and 10.1.1 missing required field (certification scenario 5.4.1). Request omits `subject`; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationMissingSubjectTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationMissingSubjectTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
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
		// Section 6.1: subject is REQUIRED.
		return new String[] {"AUTHZEN-6.1"};
	}
}
