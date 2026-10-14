package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-missing-subject",
	displayName = "AuthZEN Subject Search API - Section 10.1.1: Missing subject -- expect HTTP 400",
	summary = "Section 10.1.1 missing required field. The Subject Search request omits `subject`, which Section 8.4.1 requires to name the searched-for type; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchMissingSubjectTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchMissingSubjectTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"action": { "name": "read" },
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
