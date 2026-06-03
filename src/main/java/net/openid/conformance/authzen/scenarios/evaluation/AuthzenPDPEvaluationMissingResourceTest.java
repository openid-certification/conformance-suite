package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-missing-resource",
	displayName = "Authzen Evaluation API - Section 10.1: Missing resource -- expect HTTP 400",
	summary = "Section 10.1 missing required field. Request omits `resource`; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationMissingResourceTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationMissingResourceTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" }
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
