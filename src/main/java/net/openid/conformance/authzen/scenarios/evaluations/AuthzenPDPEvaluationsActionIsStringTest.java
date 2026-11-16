package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-action-is-string",
	displayName = "AuthZEN Evaluations API - Section 5.3: Top-level action is string -- expect HTTP 400",
	summary = "Section 5.3 invalid field type in a batch (certification scenario 5.4.6). The top-level `action` default is the string \"read\" instead of an object. Certification scenario 3.1 applies the error handling of scenario 5.4 to every level. "
		+ "A default that is not an object at all cannot be read, so the whole payload is malformed and no evaluation can be completed. The PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationsActionIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsActionIsStringTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": "read",
			"evaluations": [
				{ "resource": { "type": "record", "id": "record-1" } }
			]
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

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 5.3: "Action is an object".
		return new String[] {"AUTHZEN-5.3"};
	}
}
