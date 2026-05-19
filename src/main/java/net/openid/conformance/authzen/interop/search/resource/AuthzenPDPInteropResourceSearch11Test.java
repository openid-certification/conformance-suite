package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-11",
	displayName = "Authzen Resource Search API Test 11",
	summary = "Authzen Resource Search API test 11 with payload\n" + AuthzenPDPInteropResourceSearch11Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch11Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "dan"
		},
		"action": {
			"name": "edit"
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
					"id": "115"
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
