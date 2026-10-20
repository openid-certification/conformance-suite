package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-action-name-is-number",
	displayName = "AuthZEN Subject Search API - Section 5.3: Action name is number -- expect HTTP 400",
	summary = "Section 5.3 invalid field type (certification scenario 5.4.6, which scenario 3.1 applies to every level). `action.name` is a number instead of a string; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchActionNameIsNumberTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchActionNameIsNumberTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": 123 },
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
		// Section 5.3: an Action's `name` has "a string value".
		return new String[] {"AUTHZEN-5.3"};
	}
}
