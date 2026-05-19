package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-86",
	displayName = "Authzen Action Search API Test 86",
	summary = "Authzen Action Search API test 86 with payload\n" + AuthzenPDPInteropActionSearch86Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch86Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "erin"
			},
			"resource": {
				"type": "record",
				"id": "106"
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
