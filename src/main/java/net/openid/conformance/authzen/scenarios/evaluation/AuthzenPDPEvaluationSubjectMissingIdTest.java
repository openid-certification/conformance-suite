package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-subject-missing-id",
	displayName = "Authzen Evaluation API - Section 2.4.2: Subject missing id -- expect HTTP 400",
	summary = "Section 2.4.2 missing required sub-field. `subject` omits `id`; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationSubjectMissingIdTest.payload,
	profile = "Authzen"
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
}
