package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-16",
	displayName = "Authzen Resource Search API Test 16",
	summary = "Authzen Resource Search API test 16 with payload\n" + AuthzenPDPInteropResourceSearch16Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch16Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "felix"
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
					"id": "104"
				},
				{
					"type": "record",
					"id": "106"
				},
				{
					"type": "record",
					"id": "109"
				},
				{
					"type": "record",
					"id": "111"
				},
				{
					"type": "record",
					"id": "112"
				},
				{
					"type": "record",
					"id": "114"
				},
				{
					"type": "record",
					"id": "118"
				},
				{
					"type": "record",
					"id": "120"
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
