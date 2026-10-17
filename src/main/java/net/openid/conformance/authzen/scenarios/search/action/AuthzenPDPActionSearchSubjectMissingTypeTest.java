package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-subject-missing-type",
	displayName = "AuthZEN Action Search API - Section 10.1.1: Subject missing type -- expect HTTP 400",
	summary = "Sections 5.1 and 10.1.1 missing required sub-field (certification scenario 7.7.2). Action Search subject omits `type`; PDP MUST return HTTP 400.\n" + AuthzenPDPActionSearchSubjectMissingTypeTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchSubjectMissingTypeTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "id": "alice" },
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
		// Section 5.1: a Subject has two REQUIRED keys, type and id.
		return new String[] {"AUTHZEN-5.1"};
	}
}
