package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-missing-resource",
	displayName = "AuthZEN Action Search API - Section 10.1.1: Missing resource -- expect HTTP 400",
	summary = "Sections 8.6.1 and 10.1.1 missing required field (certification scenario 7.7.1). Action Search request omits `resource`; PDP MUST return HTTP 400.\n" + AuthzenPDPActionSearchMissingResourceTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchMissingResourceTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" }
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
		// Section 8.6.1: subject and resource are REQUIRED.
		return new String[] {"AUTHZEN-8.6.1"};
	}
}
