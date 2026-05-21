package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-request-with-unknown-fields",
	displayName = "Authzen Evaluation API - Section 2.2.9: Request with unknown fields",
	summary = "Section 2.2.9 request with unknown fields. Per forward-compatibility, the PDP MUST ignore unrecognised fields and return {\"decision\": true}.\n" + AuthzenPDPEvaluationRequestWithUnknownFieldsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationRequestWithUnknownFieldsTest extends AbstractAuthzenPDPEvaluationTest {

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
	protected String getExpectedDecisionResponseJson() {
		return """
			{ "decision": true }
			""";
	}
}
