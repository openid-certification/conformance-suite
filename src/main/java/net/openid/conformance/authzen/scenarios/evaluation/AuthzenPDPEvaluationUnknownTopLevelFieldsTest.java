package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-unknown-top-level-fields-ignored",
	displayName = "Authzen Evaluation API - Section 10.1.1: Unknown top-level fields ignored",
	summary = "Per Section 10.1.1, receivers MUST ignore unknown fields. Adds `foo` and `futureField` at the top level alongside a fixture request; PDP MUST return HTTP 200 with the correct decision.\n" + AuthzenPDPEvaluationUnknownTopLevelFieldsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationUnknownTopLevelFieldsTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"foo": "bar",
			"futureField": { "nested": true }
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
