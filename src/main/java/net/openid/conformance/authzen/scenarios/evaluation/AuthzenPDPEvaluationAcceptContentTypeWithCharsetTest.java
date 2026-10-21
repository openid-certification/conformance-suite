package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-accept-content-type-with-charset",
	displayName = "AuthZEN Evaluation API - Section 10.1: Accept Content-Type with charset",
	summary = "Section 10.1 has the PEP send the Content-Type application/json; this request sends `application/json; charset=utf-8`. RFC 8259 Section 11 defines no charset parameter for application/json and says \"Adding one really has no effect on compliant recipients\", so the PDP MUST accept it and return the expected decision. Whether a PDP may reject the parameter is an open question for the working group.",
	profile = "AuthZEN"
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
