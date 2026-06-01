package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-subject-missing-type",
	displayName = "Authzen Subject Search API - Section 4.7.2: Subject missing type -- expect HTTP 400",
	summary = "Section 4.7.2 missing required sub-field. Subject Search subject omits `type`; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchSubjectMissingTypeTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchSubjectMissingTypeTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": {},
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
