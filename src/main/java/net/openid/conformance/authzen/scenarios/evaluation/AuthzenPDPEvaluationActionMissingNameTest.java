package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-action-missing-name",
	displayName = "Authzen Evaluation API - Section 2.4.2: Action missing name -- expect HTTP 400",
	summary = "Section 2.4.2 missing required sub-field. `action` is an empty object; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationActionMissingNameTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationActionMissingNameTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": {},
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
