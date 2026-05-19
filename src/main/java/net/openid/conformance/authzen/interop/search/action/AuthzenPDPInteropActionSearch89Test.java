package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-89",
	displayName = "Authzen Action Search API Test 89",
	summary = "Authzen Action Search API test 89 with payload\n" + AuthzenPDPInteropActionSearch89Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch89Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "erin"
			},
			"resource": {
				"type": "record",
				"id": "109"
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
