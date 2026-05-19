package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-70",
	displayName = "Authzen Action Search API Test 70",
	summary = "Authzen Action Search API test 70 with payload\n" + AuthzenPDPInteropActionSearch70Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch70Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "dan"
			},
			"resource": {
				"type": "record",
				"id": "110"
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
				},
				{
					"name": "delete"
				}
			]
		}""";
	}

	@Override
	protected String getPayload() {
		return payload;
	}

}
