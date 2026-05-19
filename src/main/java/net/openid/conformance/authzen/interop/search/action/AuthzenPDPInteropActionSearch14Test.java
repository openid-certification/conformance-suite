package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-14",
	displayName = "Authzen Action Search API Test 14",
	summary = "Authzen Action Search API test 14 with payload\n" + AuthzenPDPInteropActionSearch14Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch14Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "alice"
			},
			"resource": {
				"type": "record",
				"id": "114"
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
