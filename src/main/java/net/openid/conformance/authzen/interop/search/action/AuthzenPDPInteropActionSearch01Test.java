package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-01",
	displayName = "Authzen Action Search API Test 01",
	summary = "Authzen Action Search API test 01 with payload\n" + AuthzenPDPInteropActionSearch01Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch01Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "alice"
		},
		"resource": {
			"type": "record",
			"id": "101"
		}
	}
	""";

	@Override
	protected String getExpectedSearchResponseJson() {
		return """
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
		}
		""";
	}

	@Override
	protected String getPayload() {
		return payload;
	}

}
