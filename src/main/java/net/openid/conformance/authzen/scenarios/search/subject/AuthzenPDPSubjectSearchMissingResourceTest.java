package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-missing-resource",
	displayName = "AuthZEN Subject Search API - Section 10.1.1: Missing resource -- expect HTTP 400",
	summary = "Sections 8.4.1 and 10.1.1 missing required field (certification scenario 7.7.1). Subject Search request omits `resource`; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchMissingResourceTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchMissingResourceTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" }
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
