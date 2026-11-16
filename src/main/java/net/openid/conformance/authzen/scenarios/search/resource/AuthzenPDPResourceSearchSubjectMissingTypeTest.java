package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-subject-missing-type",
	displayName = "AuthZEN Resource Search API - Section 10.1.1: Subject missing type -- expect HTTP 400",
	summary = "Sections 5.1 and 10.1.1 missing required sub-field (certification scenario 7.7.2). Resource Search subject omits `type`; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchSubjectMissingTypeTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchSubjectMissingTypeTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "id": "alice" },
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
		// Section 5.1: a Subject has two REQUIRED keys, type and id.
		return new String[] {"AUTHZEN-5.1"};
	}
}
