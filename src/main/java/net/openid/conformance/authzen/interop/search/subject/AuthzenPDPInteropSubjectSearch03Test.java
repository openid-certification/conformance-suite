package net.openid.conformance.authzen.interop.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-subject-search-03",
	displayName = "Authzen Subject Search API Test 03",
	summary = "Subject Search API test 03 with payload\n" + AuthzenPDPInteropSubjectSearch03Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropSubjectSearch03Test extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"resource": {
				"type": "record",
				"id": "101"
			},
			"action": {
				"name": "delete"
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
				}
			]
		}
		""";
	}
}
