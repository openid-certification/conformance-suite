package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-06",
	displayName = "Authzen Resource Search API Test 06",
	summary = "Authzen Resource Search API test 06 with payload\n" + AuthzenPDPInteropResourceSearch06Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch06Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "bob"
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
