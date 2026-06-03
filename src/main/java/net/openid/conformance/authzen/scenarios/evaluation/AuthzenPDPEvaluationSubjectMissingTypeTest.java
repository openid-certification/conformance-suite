package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-subject-missing-type",
	displayName = "Authzen Evaluation API - Section 10.1: Subject missing type -- expect HTTP 400",
	summary = "Section 10.1 missing required sub-field. `subject` omits `type`; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationSubjectMissingTypeTest.payload,
	profile = "Authzen"
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
}
