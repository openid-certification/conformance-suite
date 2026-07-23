package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-96",
	displayName = "AuthZEN Action Search API Test 96",
	summary = "AuthZEN Action Search API test 96 with payload\n" + AuthzenPDPInteropActionSearch96Test.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPInteropActionSearch96Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "erin"
			},
			"resource": {
				"type": "record",
				"id": "116"
			}
		}""";

	@Override
	protected String getExpectedSearchResponseJson() { return """
		{
			"results": []
		}""";
	}

	@Override
	protected String getPayload() {
		return payload;
	}

}
