package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-05",
	displayName = "Authzen Resource Search API Test 05",
	summary = "Authzen Resource Search API test 05 with payload\n" + AuthzenPDPInteropResourceSearch05Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch05Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "bob"
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
					"id": "102"
				},
				{
					"type": "record",
					"id": "108"
				},
				{
					"type": "record",
					"id": "114"
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
