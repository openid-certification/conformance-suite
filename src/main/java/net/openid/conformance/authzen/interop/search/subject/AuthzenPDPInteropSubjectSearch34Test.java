package net.openid.conformance.authzen.interop.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-subject-search-34",
	displayName = "Authzen Subject Search API Test 34",
	summary = "Subject Search API test 34 with payload\n" + AuthzenPDPInteropSubjectSearch34Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropSubjectSearch34Test extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
	{
		"resource": {
			"type": "record",
			"id": "112"
		},
		"action": {
			"name": "view"
		},
		"subject": {
			"type": "user"
		}
	}
	""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return """
		{
			"results": [
				{
					"type": "user",
					"id": "alice"
				},
				{
					"type": "user",
					"id": "bob"
				},
				{
					"type": "user",
					"id": "carol"
				},
				{
					"type": "user",
					"id": "dan"
				},
				{
					"type": "user",
					"id": "felix"
				}
			]
		}
		""";
	}
}
