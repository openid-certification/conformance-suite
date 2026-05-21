package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-unknown-subject-fields-ignored",
	displayName = "Authzen Evaluation API - Spec 10.1.1-3: Unknown subject fields ignored",
	summary = "Per spec 10.1.1-3, receivers MUST ignore unknown fields. `subject` carries an unknown `customAttr` field; PDP MUST return HTTP 200 with the correct decision.\n" + AuthzenPDPEvaluationUnknownSubjectFieldsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationUnknownSubjectFieldsTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "alice",
				"customAttr": "ignored",
				"anotherUnknown": { "k": "v" }
			},
			"action": { "name": "read" },
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
