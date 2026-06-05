package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-resource-missing-type",
	displayName = "Authzen Action Search API - Section 10.1.1: Resource missing type -- expect HTTP 400",
	summary = "Section 10.1.1 missing required sub-field. Action Search resource omits `type`; PDP MUST return HTTP 400.\n" + AuthzenPDPActionSearchResourceMissingTypeTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPActionSearchResourceMissingTypeTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"resource": { "id": "record-1" }
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
