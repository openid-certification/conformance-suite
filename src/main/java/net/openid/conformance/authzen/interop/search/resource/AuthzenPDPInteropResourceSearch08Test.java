package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-08",
	displayName = "Authzen Resource Search API Test 08",
	summary = "Authzen Resource Search API test 08 with payload\n" + AuthzenPDPInteropResourceSearch08Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch08Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "carol"
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
					"id": "103"
				},
				{
					"type": "record",
					"id": "109"
				},
				{
					"type": "record",
					"id": "115"
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
