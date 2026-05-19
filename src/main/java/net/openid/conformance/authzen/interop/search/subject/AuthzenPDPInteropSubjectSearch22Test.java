package net.openid.conformance.authzen.interop.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-subject-search-22",
	displayName = "Authzen Subject Search API Test 22",
	summary = "Subject Search API test 22 with payload\n" + AuthzenPDPInteropSubjectSearch22Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropSubjectSearch22Test extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
	{
		"resource": {
			"type": "record",
			"id": "108"
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
				}
			]
		}
		""";
	}
}
