package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-10",
	displayName = "Authzen Resource Search API Test 10",
	summary = "Authzen Resource Search API test 10 with payload\n" + AuthzenPDPInteropResourceSearch10Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch10Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "dan"
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
