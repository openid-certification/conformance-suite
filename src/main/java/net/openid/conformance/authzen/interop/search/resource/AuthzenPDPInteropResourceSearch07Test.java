package net.openid.conformance.authzen.interop.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-resource-search-07",
	displayName = "Authzen Resource Search API Test 07",
	summary = "Authzen Resource Search API test 07 with payload\n" + AuthzenPDPInteropResourceSearch07Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropResourceSearch07Test extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "carol"
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
					"id": "109"
				},
				{
					"type": "record",
					"id": "112"
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
					"id": "119"
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
