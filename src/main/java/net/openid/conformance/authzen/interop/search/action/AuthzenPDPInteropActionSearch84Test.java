package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-84",
	displayName = "Authzen Action Search API Test 84",
	summary = "Authzen Action Search API test 84 with payload\n" + AuthzenPDPInteropActionSearch84Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch84Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "erin"
			},
			"resource": {
				"type": "record",
				"id": "104"
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
