package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-75",
	displayName = "Authzen Action Search API Test 75",
	summary = "Authzen Action Search API test 75 with payload\n" + AuthzenPDPInteropActionSearch75Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch75Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "dan"
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
				},
				{
					"name": "edit"
				}
			]
		}""";
	}

	@Override
	protected String getPayload() {
		return payload;
	}

}
