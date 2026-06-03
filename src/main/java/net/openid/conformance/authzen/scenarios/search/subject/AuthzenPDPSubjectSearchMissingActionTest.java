package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-missing-action",
	displayName = "Authzen Subject Search API - Section 10.1.1: Missing action -- expect HTTP 400",
	summary = "Section 10.1.1 missing required field. Subject Search request omits `action`; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchMissingActionTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchMissingActionTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"resource": { "type": "record", "id": "record-1" }
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
