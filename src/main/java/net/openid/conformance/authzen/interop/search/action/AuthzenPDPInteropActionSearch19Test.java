package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-19",
	displayName = "Authzen Action Search API Test 19",
	summary = "Authzen Action Search API test 19 with payload\n" + AuthzenPDPInteropActionSearch19Test.payload,
	profile = "Authzen"
)
public class AuthzenPDPInteropActionSearch19Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "alice"
			},
			"resource": {
				"type": "record",
				"id": "119"
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
