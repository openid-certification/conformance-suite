package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-action-name-is-number",
	displayName = "Authzen Evaluation API - Section 2.4.6: Action name is number -- expect HTTP 400",
	summary = "Section 2.4.6 invalid field type. `action.name` is a number instead of a string; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationActionNameIsNumberTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationActionNameIsNumberTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": 123 },
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
