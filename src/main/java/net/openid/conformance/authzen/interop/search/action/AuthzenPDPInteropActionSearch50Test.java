package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-50",
	displayName = "Authzen Action Search API Test 50",
	summary = "Authzen Action Search API test 50 with payload\n" + AuthzenPDPInteropActionSearch50Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch50Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "carol"
			},
			"resource": {
				"type": "record",
				"id": "110"
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
