package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-115",
	displayName = "Authzen Action Search API Test 115",
	summary = "Authzen Action Search API test 115 with payload\n" + AuthzenPDPInteropActionSearch115Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch115Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "felix"
			},
			"resource": {
				"type": "record",
				"id": "115"
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
