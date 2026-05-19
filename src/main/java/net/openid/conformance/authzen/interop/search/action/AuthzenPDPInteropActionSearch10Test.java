package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-10",
	displayName = "Authzen Action Search API Test 10",
	summary = "Authzen Action Search API test 10 with payload\n" + AuthzenPDPInteropActionSearch10Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch10Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "alice"
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
				}
			]
		}""";
	}

	@Override
	protected String getPayload() {
		return payload;
	}

}
