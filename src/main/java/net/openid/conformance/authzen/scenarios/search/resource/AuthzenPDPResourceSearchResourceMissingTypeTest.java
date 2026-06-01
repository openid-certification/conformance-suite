package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-resource-missing-type",
	displayName = "Authzen Resource Search API - Section 4.7.2: Resource missing type -- expect HTTP 400",
	summary = "Section 4.7.2 missing required sub-field. Resource Search resource omits `type`; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchResourceMissingTypeTest.payload,
	profile = "Authzen"
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
