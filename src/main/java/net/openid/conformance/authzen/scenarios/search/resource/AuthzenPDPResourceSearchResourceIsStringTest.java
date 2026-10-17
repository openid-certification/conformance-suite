package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-resource-is-string",
	displayName = "AuthZEN Resource Search API - Section 5.2: Resource is a string value -- expect HTTP 400",
	summary = "Section 5.2 invalid field type (certification scenario 5.4.6). The Resource Search resource is sent as the string \"record\" rather than the required JSON object; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchResourceIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchResourceIsStringTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": "record"
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
		// Section 5.2: a Resource "is an object".
		return new String[] {"AUTHZEN-5.2"};
	}
}
