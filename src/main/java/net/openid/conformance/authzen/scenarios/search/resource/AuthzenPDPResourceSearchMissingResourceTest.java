package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-missing-resource",
	displayName = "Authzen Resource Search API - Section 10.1.1: Missing resource -- expect HTTP 400",
	summary = "Section 10.1.1 missing required field. Resource Search request omits `resource`; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchMissingResourceTest.payload,
	profile = "Authzen"
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
	protected String getExpectedSearchResponseJson() {
		return "{ \"results\": [] }";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}
}
