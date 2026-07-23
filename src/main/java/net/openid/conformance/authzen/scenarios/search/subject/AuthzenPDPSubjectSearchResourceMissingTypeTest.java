package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-resource-missing-type",
	displayName = "AuthZEN Subject Search API - Section 10.1.1: Resource missing type -- expect HTTP 400",
	summary = "Section 10.1.1 missing required sub-field. Subject Search resource omits `type`; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchResourceMissingTypeTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchResourceMissingTypeTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
			"resource": { "id": "record-1" }
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
