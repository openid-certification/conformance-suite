package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-108",
	displayName = "Authzen Action Search API Test 108",
	summary = "Authzen Action Search API test 108 with payload\n" + AuthzenPDPInteropActionSearch108Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch108Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "felix"
			},
			"resource": {
				"type": "record",
				"id": "108"
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
