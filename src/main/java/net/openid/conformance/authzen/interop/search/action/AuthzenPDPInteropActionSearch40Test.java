package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-40",
	displayName = "Authzen Action Search API Test 40",
	summary = "Authzen Action Search API test 40 with payload\n" + AuthzenPDPInteropActionSearch40Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch40Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "bob"
			},
			"resource": {
				"type": "record",
				"id": "120"
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
