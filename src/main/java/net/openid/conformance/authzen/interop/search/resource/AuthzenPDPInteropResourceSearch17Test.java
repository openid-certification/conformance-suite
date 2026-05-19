package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-17",
	displayName = "Authzen Resource Search API Test 17",
	summary = "Authzen Resource Search API test 17 with payload\n" + AuthzenPDPInteropResourceSearch17Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch17Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "felix"
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
					"id": "106"
				},
				{
					"type": "record",
					"id": "112"
				},
				{
					"type": "record",
					"id": "118"
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
