package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-action-is-string",
	displayName = "AuthZEN Resource Search API - Section 5.3: Action is a string value -- expect HTTP 400",
	summary = "Section 5.3 invalid field type (certification scenario 5.4.6, which scenario 3.1 applies to every level). The Resource Search action is sent as the string \"read\" rather than the required JSON object; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchActionIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchActionIsStringTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": "read",
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
		// Section 5.3: "Action is an object".
		return new String[] {"AUTHZEN-5.3"};
	}
}
