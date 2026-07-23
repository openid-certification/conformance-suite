package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-12",
	displayName = "AuthZEN Resource Search API Test 12",
	summary = "AuthZEN Resource Search API test 12 with payload\n" + AuthzenPDPInteropResourceSearch12Test.payload,
	profile = "AuthZEN",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch12Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "dan"
		},
		"action": {
			"name": "delete"
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
					"id": "104"
				},
				{
					"type": "record",
					"id": "110"
				},
				{
					"type": "record",
					"id": "116"
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
