package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-missing-subject",
	displayName = "Authzen Resource Search API - Section 4.7.1: Missing subject -- expect HTTP 400",
	summary = "Section 4.7.1 missing required field. Resource Search request omits `subject`; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchMissingSubjectTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchMissingSubjectTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"action": { "name": "read" },
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
