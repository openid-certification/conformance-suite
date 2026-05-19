package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-13",
	displayName = "Authzen Action Search API Test 13",
	summary = "Authzen Action Search API test 13 with payload\n" + AuthzenPDPInteropActionSearch13Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch13Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "alice"
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
