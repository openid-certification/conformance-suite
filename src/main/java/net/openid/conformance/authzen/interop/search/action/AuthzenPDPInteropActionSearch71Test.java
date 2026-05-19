package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-71",
	displayName = "Authzen Action Search API Test 71",
	summary = "Authzen Action Search API test 71 with payload\n" + AuthzenPDPInteropActionSearch71Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch71Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "dan"
			},
			"resource": {
				"type": "record",
				"id": "11"
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
