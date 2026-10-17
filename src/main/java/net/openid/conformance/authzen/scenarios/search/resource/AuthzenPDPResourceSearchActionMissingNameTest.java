package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-action-missing-name",
	displayName = "AuthZEN Resource Search API - Section 10.1.1: Action missing name -- expect HTTP 400",
	summary = "Sections 5.3 and 10.1.1 missing required sub-field (certification scenario 7.7.2). Resource Search action omits `name`; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchActionMissingNameTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchActionMissingNameTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": {},
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
		// Section 5.3: an Action has a REQUIRED name key.
		return new String[] {"AUTHZEN-5.3"};
	}
}
