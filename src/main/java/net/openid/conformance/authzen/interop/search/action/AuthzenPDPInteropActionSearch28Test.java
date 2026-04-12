package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-28",
	displayName = "Authzen Action Search API Test 28",
	summary = "Authzen Action Search API test 28 with payload\n" + AuthzenPDPInteropActionSearch28Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch28Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "bob"
			},
			"resource": {
				"type": "record",
				"id": "108"
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
