package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-subject-is-string",
	displayName = "Authzen Action Search API - Section 4.7: Subject is a string value -- expect HTTP 400",
	summary = "Action Search subject is sent as the string \"alice\" rather than the required JSON object; PDP MUST return HTTP 400.\n" + AuthzenPDPActionSearchSubjectIsStringTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPActionSearchSubjectIsStringTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": "alice",
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
