package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-action-missing-name",
	displayName = "AuthZEN Evaluation API - Section 10.1.1: Action missing name -- expect HTTP 400",
	summary = "Sections 5.3 and 10.1.1 missing required sub-field (certification scenario 5.4.2). `action` is an empty object; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationActionMissingNameTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPEvaluationActionMissingNameTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": {},
			"resource": { "type": "record", "id": "record-1" }
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
		// Section 5.3: an Action has a REQUIRED name key.
		return new String[] {"AUTHZEN-5.3"};
	}
}
