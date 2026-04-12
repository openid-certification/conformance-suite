package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-95",
	displayName = "Authzen Action Search API Test 95",
	summary = "Authzen Action Search API test 95 with payload\n" + AuthzenPDPInteropActionSearch95Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch95Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "erin"
			},
			"resource": {
				"type": "record",
				"id": "115"
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
