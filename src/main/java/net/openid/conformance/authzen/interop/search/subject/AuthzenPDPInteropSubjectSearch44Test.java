package net.openid.conformance.authzen.interop.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-subject-search-44",
	displayName = "Authzen Subject Search API Test 44",
	summary = "Subject Search API test 44 with payload\n" + AuthzenPDPInteropSubjectSearch44Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropSubjectSearch44Test extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
	{
		"resource": {
			"type": "record",
			"id": "115"
		},
		"action": {
			"name": "edit"
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
