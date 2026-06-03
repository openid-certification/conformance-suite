package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-action-is-string",
	displayName = "Authzen Subject Search API - Section 10.1: Action is a string value -- expect HTTP 400",
	summary = "Subject Search action is sent as the string \"read\" rather than the required JSON object; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchActionIsStringTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchActionIsStringTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": "read",
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
