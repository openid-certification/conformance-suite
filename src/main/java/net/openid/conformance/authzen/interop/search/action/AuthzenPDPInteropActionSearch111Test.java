package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-111",
	displayName = "Authzen Action Search API Test 111",
	summary = "Authzen Action Search API test 111 with payload\n" + AuthzenPDPInteropActionSearch111Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch111Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "felix"
			},
			"resource": {
				"type": "record",
				"id": "111"
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
