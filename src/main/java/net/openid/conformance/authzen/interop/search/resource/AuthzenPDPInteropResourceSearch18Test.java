package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-18",
	displayName = "AuthZEN Resource Search API Test 18",
	summary = "AuthZEN Resource Search API test 18 with payload\n" + AuthzenPDPInteropResourceSearch18Test.payload,
	profile = "AuthZEN",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch18Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "felix"
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
