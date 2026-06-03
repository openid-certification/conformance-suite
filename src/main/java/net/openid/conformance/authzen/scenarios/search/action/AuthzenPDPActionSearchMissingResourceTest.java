package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-missing-resource",
	displayName = "Authzen Action Search API - Section 10.1.1: Missing resource -- expect HTTP 400",
	summary = "Section 10.1.1 missing required field. Action Search request omits `resource`; PDP MUST return HTTP 400.\n" + AuthzenPDPActionSearchMissingResourceTest.payload,
	profile = "Authzen"
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
