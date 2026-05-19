package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-01",
	displayName = "Authzen Resource Search API Test 01",
	summary = "Authzen Resource Search API test 01 with payload\n" + AuthzenPDPInteropResourceSearch01Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch01Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "alice"
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
					"id": "104"
				},
				{
					"type": "record",
					"id": "105"
				},
				{
					"type": "record",
					"id": "106"
				},
				{
					"type": "record",
					"id": "107"
				},
				{
					"type": "record",
					"id": "108"
				},
				{
					"type": "record",
					"id": "109"
				},
				{
					"type": "record",
					"id": "110"
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
					"id": "113"
				},
				{
					"type": "record",
					"id": "114"
				},
				{
					"type": "record",
					"id": "115"
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
					"id": "118"
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
