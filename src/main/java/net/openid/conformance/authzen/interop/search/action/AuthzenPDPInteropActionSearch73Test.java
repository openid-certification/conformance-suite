package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-73",
	displayName = "Authzen Action Search API Test 73",
	summary = "Authzen Action Search API test 73 with payload\n" + AuthzenPDPInteropActionSearch73Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch73Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "dan"
			},
			"resource": {
				"type": "record",
				"id": "113"
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
