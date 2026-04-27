package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-04",
	displayName = "Authzen Resource Search API Test 04",
	summary = "Authzen Resource Search API test 04 with payload\n" + AuthzenPDPInteropResourceSearch04Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch04Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "bob"
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
					"id": "101"
				},
				{
					"type": "record",
					"id": "102"
				},
				{
					"type": "record",
					"id": "103"
				},
				{
					"type": "record",
					"id": "105"
				},
				{
					"type": "record",
					"id": "108"
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
					"id": "116"
				},
				{
					"type": "record",
					"id": "117"
				},
				{
					"type": "record",
					"id": "119"
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
