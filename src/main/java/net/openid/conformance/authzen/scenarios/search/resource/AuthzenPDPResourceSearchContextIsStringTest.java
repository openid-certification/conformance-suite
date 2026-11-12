package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-context-is-string",
	displayName = "AuthZEN Resource Search API - Section 5.4: Context is a string value -- expect HTTP 400",
	summary = "Section 5.4 invalid field type (certification scenario 5.4.6, which scenario 3.1 applies to every level). Section 8.5 defines the Resource Search API by the Context (Section 5.4) it is given, and Section 5.4 makes Context an object, so the `context` of a Resource Search request is one too; openid/authzen#803 asks for Section 8.5.1 to say so. It is sent as the string \"ip=192.168.1.1\" instead; PDP MUST return HTTP 400.\n" + AuthzenPDPResourceSearchContextIsStringTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchContextIsStringTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" },
			"context": "ip=192.168.1.1"
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
		// Section 8.5: the API is "permitted according to the provided ... Context (Section 5.4)";
		// Section 5.4: "Context is an object".
		return new String[] {"AUTHZEN-8.5", "AUTHZEN-5.4"};
	}
}
