package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-missing-resource",
	displayName = "AuthZEN Resource Search API - Section 10.1.1: Missing resource -- expect HTTP 400",
	summary = "Sections 8.5.1 and 10.1.1 missing required field (certification scenario 7.7.1). Resource Search request omits `resource`; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchMissingResourceTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchMissingResourceTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
		// Section 8.5.1: subject, action and resource (with a type) are REQUIRED.
		return new String[] {"AUTHZEN-8.5.1"};
	}
}
