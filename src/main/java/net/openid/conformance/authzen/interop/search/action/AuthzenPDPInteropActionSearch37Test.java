package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-37",
	displayName = "AuthZEN Action Search API Test 37",
	summary = "AuthZEN Action Search API test 37 with payload\n" + AuthzenPDPInteropActionSearch37Test.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPInteropActionSearch37Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "bob"
			},
			"resource": {
				"type": "record",
				"id": "117"
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
