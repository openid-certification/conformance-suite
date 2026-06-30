package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-32",
	displayName = "AuthZEN Action Search API Test 32",
	summary = "AuthZEN Action Search API test 32 with payload\n" + AuthzenPDPInteropActionSearch32Test.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPInteropActionSearch32Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "bob"
			},
			"resource": {
				"type": "record",
				"id": "112"
			}
		}""";

	@Override
	protected String getExpectedSearchResponseJson() { return """
		{
			"results": [
				{
					"name": "view"
				}
			]
		}""";
	}

	@Override
	protected String getPayload() {
		return payload;
	}

}
