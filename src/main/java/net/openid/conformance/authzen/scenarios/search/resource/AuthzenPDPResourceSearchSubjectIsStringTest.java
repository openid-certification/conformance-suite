package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-subject-is-string",
	displayName = "AuthZEN Resource Search API - Section 5.1: Subject is a string value -- expect HTTP 400",
	summary = "Section 5.1 invalid field type (certification scenario 5.4.6, which scenario 3.1 applies to every level). The Resource Search subject is sent as the string \"alice\" rather than the required JSON object; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchSubjectIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchSubjectIsStringTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": "alice",
			"action": { "name": "read" },
			"resource": { "type": "record" }
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
