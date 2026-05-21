package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-missing-action",
	displayName = "Authzen Evaluation API - Section 2.4.1: Missing action -- expect HTTP 400",
	summary = "Section 2.4.1 missing required field. Request omits `action`; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationMissingActionTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationMissingActionTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
