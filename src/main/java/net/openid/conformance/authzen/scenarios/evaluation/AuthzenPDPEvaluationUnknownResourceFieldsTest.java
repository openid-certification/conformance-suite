package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-unknown-resource-fields-ignored",
	displayName = "Authzen Evaluation API - Section 10.1.1: Unknown resource fields ignored",
	summary = "Per spec 10.1.1-3, receivers MUST ignore unknown fields. `resource` carries an unknown `customAttr`; PDP MUST return HTTP 200 with the correct decision.\n" + AuthzenPDPEvaluationUnknownResourceFieldsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationUnknownResourceFieldsTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": {
				"type": "record",
				"id": "record-1",
				"customAttr": "ignored",
				"anotherUnknown": [1, 2, 3]
			}
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
