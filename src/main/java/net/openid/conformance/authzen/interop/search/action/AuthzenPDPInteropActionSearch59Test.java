package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-59",
	displayName = "Authzen Action Search API Test 59",
	summary = "Authzen Action Search API test 59 with payload\n" + AuthzenPDPInteropActionSearch59Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch59Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "carol"
			},
			"resource": {
				"type": "record",
				"id": "119"
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
