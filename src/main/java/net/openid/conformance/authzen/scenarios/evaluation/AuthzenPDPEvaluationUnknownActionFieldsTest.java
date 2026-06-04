package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-unknown-action-fields-ignored",
	displayName = "Authzen Evaluation API - Section 10.1.1: Unknown action fields ignored",
	summary = "Per Section 10.1.1, receivers MUST ignore unknown fields. `action` carries an unknown `customAttr`; PDP MUST return HTTP 200 with the correct decision.\n" + AuthzenPDPEvaluationUnknownActionFieldsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationUnknownActionFieldsTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": {
				"name": "read",
				"customAttr": "ignored"
			},
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getExpectedDecisionResponseJson() {
		return """
			{ "decision": true }
			""";
	}
}
