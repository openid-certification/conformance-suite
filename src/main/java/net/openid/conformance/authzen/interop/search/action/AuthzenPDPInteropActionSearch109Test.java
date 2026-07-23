package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-109",
	displayName = "AuthZEN Action Search API Test 109",
	summary = "AuthZEN Action Search API test 109 with payload\n" + AuthzenPDPInteropActionSearch109Test.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPInteropActionSearch109Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "felix"
			},
			"resource": {
				"type": "record",
				"id": "109"
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
