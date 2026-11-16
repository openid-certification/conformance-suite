package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-missing-action",
	displayName = "AuthZEN Resource Search API - Section 10.1.1: Missing action -- expect HTTP 400",
	summary = "Sections 8.5.1 and 10.1.1 missing required field (certification scenario 7.7.1). Resource Search request omits `action`; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchMissingActionTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchMissingActionTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"resource": { "type": "record" }
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
