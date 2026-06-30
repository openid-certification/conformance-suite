package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-13",
	displayName = "AuthZEN Resource Search API Test 13",
	summary = "AuthZEN Resource Search API test 13 with payload\n" + AuthzenPDPInteropResourceSearch13Test.payload,
	profile = "AuthZEN",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch13Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "erin"
		},
		"action": {
			"name": "view"
		},
		"resource": {
			"type": "record"
		}
	}
	""";

	@Override
	protected String getExpectedSearchResponseJson() {
		return """
		{
			"results": [
				{
					"type": "record",
					"id": "105"
				},
				{
					"type": "record",
					"id": "111"
				},
				{
					"type": "record",
					"id": "115"
				},
				{
					"type": "record",
					"id": "117"
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
