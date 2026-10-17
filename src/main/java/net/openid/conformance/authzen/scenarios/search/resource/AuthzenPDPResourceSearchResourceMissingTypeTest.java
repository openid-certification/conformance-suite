package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-resource-missing-type",
	displayName = "AuthZEN Resource Search API - Section 10.1.1: Resource missing type -- expect HTTP 400",
	summary = "Sections 8.5.1 and 10.1.1 missing required sub-field (certification scenario 7.7.2). Resource Search resource omits `type`; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchResourceMissingTypeTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchResourceMissingTypeTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": {}
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
