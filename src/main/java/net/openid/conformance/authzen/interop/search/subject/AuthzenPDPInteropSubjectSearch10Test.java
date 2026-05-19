package net.openid.conformance.authzen.interop.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-subject-search-10",
	displayName = "Authzen Subject Search API Test 10",
	summary = "Subject Search API test 10 with payload\n" + AuthzenPDPInteropSubjectSearch10Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropSubjectSearch10Test extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
	{
		"resource": {
			"type": "record",
			"id": "104"
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
