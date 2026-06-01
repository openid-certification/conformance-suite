package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-missing-resource",
	displayName = "Authzen Subject Search API - Section 4.7.1: Missing resource -- expect HTTP 400",
	summary = "Section 4.7.1 missing required field. Subject Search request omits `resource`; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchMissingResourceTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchMissingResourceTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
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
