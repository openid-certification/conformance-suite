package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-missing-action",
	displayName = "AuthZEN Subject Search API - Section 10.1.1: Missing action -- expect HTTP 400",
	summary = "Sections 8.4.1 and 10.1.1 missing required field (certification scenario 7.7.1). Subject Search request omits `action`; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchMissingActionTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchMissingActionTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
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
		// Section 8.4.1: subject (with a type), action and resource are REQUIRED.
		return new String[] {"AUTHZEN-8.4.1"};
	}
}
