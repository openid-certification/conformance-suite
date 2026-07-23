package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-09",
	displayName = "AuthZEN Action Search API Test 09",
	summary = "AuthZEN Action Search API test 09 with payload\n" + AuthzenPDPInteropActionSearch09Test.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPInteropActionSearch09Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "alice"
			},
			"resource": {
				"type": "record",
				"id": "109"
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
