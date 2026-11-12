package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-subject-is-string",
	displayName = "AuthZEN Subject Search API - Section 5.1: Subject is a string value -- expect HTTP 400",
	summary = "Section 5.1 invalid field type (certification scenario 5.4.6, which scenario 3.1 applies to every level). The Subject Search subject is sent as the string \"alice\" rather than the required JSON object; PDP MUST return HTTP 400.\n" + AuthzenPDPSubjectSearchSubjectIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchSubjectIsStringTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": "alice",
			"action": { "name": "read" },
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
		// Section 5.1: "A Subject is an object".
		return new String[] {"AUTHZEN-5.1"};
	}
}
