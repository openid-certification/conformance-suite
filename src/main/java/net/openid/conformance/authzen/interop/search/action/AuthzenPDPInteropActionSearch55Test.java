package net.openid.conformance.authzen.interop.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-action-search-55",
	displayName = "AuthZEN Action Search API Test 55",
	summary = "AuthZEN Action Search API test 55 with payload\n" + AuthzenPDPInteropActionSearch55Test.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPInteropActionSearch55Test extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": {
				"type": "user",
				"id": "carol"
			},
			"resource": {
				"type": "record",
				"id": "115"
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
