package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-action-missing-name",
	displayName = "Authzen Subject Search API - Section 4.7.2: Action missing name -- expect HTTP 400",
	summary = "Section 4.7.2 missing required sub-field. Subject Search action omits `name`; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchActionMissingNameTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchActionMissingNameTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": {},
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
