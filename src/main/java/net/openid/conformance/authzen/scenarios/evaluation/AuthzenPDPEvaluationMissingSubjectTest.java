package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-missing-subject",
	displayName = "Authzen Evaluation API - Section 2.4.1: Missing subject -- expect HTTP 400",
	summary = "Section 2.4.1 missing required field. Request omits `subject`; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationMissingSubjectTest.payload,
	profile = "Authzen"
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
}
