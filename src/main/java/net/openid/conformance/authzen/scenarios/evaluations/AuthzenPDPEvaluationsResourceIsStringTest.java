package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-resource-is-string",
	displayName = "AuthZEN Evaluations API - Section 5.2: Top-level resource is string -- expect HTTP 400",
	summary = "Section 5.2 invalid field type in a batch (certification scenario 5.4.6). The top-level `resource` default is the string \"record-1\" instead of an object. Certification scenario 3.1 applies the error handling of scenario 5.4 to every level. "
		+ "A default that is not an object at all cannot be read, so the whole payload is malformed and no evaluation can be completed. The PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationsResourceIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationsResourceIsStringTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"resource": "record-1",
			"evaluations": [
				{ "action": { "name": "read" } }
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
		// Section 5.2: a Resource "is an object".
		return new String[] {"AUTHZEN-5.2"};
	}
}
