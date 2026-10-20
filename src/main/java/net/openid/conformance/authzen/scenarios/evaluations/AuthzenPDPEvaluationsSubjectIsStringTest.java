package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-subject-is-string",
	displayName = "AuthZEN Evaluations API - Section 5.1: Top-level subject is string -- expect HTTP 400",
	summary = "Section 5.1 invalid field type in a batch (certification scenario 5.4.6). The top-level `subject` default is the string \"alice\" instead of an object. Certification scenario 3.1 applies the error handling of scenario 5.4 to every level, and scenario 6.4.1 keeps a request that is invalid at the top level an HTTP error response. The PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationsSubjectIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsSubjectIsStringTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": "alice",
			"action": { "name": "read" },
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
		// Section 5.1: "A Subject is an object".
		return new String[] {"AUTHZEN-5.1"};
	}
}
