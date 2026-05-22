package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-accept-content-type-with-charset",
	displayName = "Authzen Evaluation API - Spec 10.1-2: Accept Content-Type with charset",
	summary = "Per spec 10.1-2 and RFC 9110, `application/json; charset=utf-8` is a valid form of the JSON Content-Type. The PDP MUST accept it and return the expected decision.",
	profile = "Authzen"
)
public class AuthzenPDPEvaluationAcceptContentTypeWithCharsetTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getRequestContentTypeOverride() {
		return "application/json; charset=utf-8";
	}

	@Override
	protected String getExpectedDecisionResponseJson() {
		return """
			{ "decision": true }
			""";
	}
}
