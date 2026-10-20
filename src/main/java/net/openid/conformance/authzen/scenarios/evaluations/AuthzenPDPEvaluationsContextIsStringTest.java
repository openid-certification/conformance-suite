package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-context-is-string",
	displayName = "AuthZEN Evaluations API - Section 5.4: Top-level context is string -- expect HTTP 400",
	summary = "Section 5.4 invalid field type in a batch (certification scenario 5.4.6). The top-level `context` default is a string instead of an object. Certification scenario 3.1 applies the error handling of scenario 5.4 to every level, and scenario 6.4.1 keeps a request that is invalid at the top level an HTTP error response. "
		+ "A default that is not an object at all cannot be read as a context, so the whole payload is malformed and no evaluation can be completed; a default object that lacks or mistypes a member (such as a subject without `type`) is instead an error each evaluation can fail on, and may be answered either way. The PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationsContextIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsContextIsStringTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"context": "2025-06-27T18:03-07:00",
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
		// Section 5.4: "Context is an object".
		return new String[] {"AUTHZEN-5.4"};
	}
}
