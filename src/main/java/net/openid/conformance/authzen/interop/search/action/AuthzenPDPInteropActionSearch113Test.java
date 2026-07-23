package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-113",
	displayName = "AuthZEN Action Search API Test 113",
	summary = "AuthZEN Action Search API test 113 with payload\n" + AuthzenPDPInteropActionSearch113Test.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPInteropActionSearch113Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "felix"
			},
			"resource": {
				"type": "record",
				"id": "113"
			}
		}""";

	@Override
	protected String getExpectedSearchResponseJson() { return """
		{
			"results": []
		}""";
	}

	@Override
	protected String getPayload() {
		return payload;
	}

}
