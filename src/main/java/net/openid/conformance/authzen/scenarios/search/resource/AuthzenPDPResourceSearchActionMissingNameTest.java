package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-action-missing-name",
	displayName = "Authzen Resource Search API - Section 4.7.2: Action missing name -- expect HTTP 400",
	summary = "Section 4.7.2 missing required sub-field. Resource Search action omits `name`; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchActionMissingNameTest.payload,
	profile = "Authzen"
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
