package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-resource-missing-type",
	displayName = "AuthZEN Evaluation API - Section 10.1.1: Resource missing type -- expect HTTP 400",
	summary = "Sections 5.2 and 10.1.1 missing required sub-field (certification scenario 5.4.2). `resource` omits `type`; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationResourceMissingTypeTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationResourceMissingTypeTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "id": "record-1" }
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
		// Section 5.2: a Resource has two REQUIRED keys, type and id.
		return new String[] {"AUTHZEN-5.2"};
	}
}
