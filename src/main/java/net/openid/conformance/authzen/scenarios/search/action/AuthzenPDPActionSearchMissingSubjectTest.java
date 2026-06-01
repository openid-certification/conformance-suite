package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-missing-subject",
	displayName = "Authzen Action Search API - Section 4.7.1: Missing subject -- expect HTTP 400",
	summary = "Section 4.7.1 missing required field. Action Search request omits `subject`; PDP MUST return HTTP 400.\n" + AuthzenPDPActionSearchMissingSubjectTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPActionSearchMissingSubjectTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
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
