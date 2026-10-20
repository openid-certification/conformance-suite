package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-subject-id-is-number",
	displayName = "AuthZEN Action Search API - Section 5.1: Subject id is number -- expect HTTP 400",
	summary = "Section 5.1 invalid field type (certification scenario 5.4.6, which scenario 3.1 applies to every level). An Action Search request carries no action, so where the other search plans send a number as `action.name`, this module sends one as `subject.id`; PDP MUST return HTTP 400.\n" + AuthzenPDPActionSearchSubjectIdIsNumberTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchSubjectIdIsNumberTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": 123 },
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String[] additionalStatusRequirements() {
		// Section 5.1: a Subject's `type` and `id` "have a string value".
		return new String[] {"AUTHZEN-5.1"};
	}
}
