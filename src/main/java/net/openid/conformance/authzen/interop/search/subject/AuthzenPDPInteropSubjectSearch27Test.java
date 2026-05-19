package net.openid.conformance.authzen.interop.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-subject-search-27",
	displayName = "Authzen Subject Search API Test 27",
	summary = "Subject Search API test 27 with payload\n" + AuthzenPDPInteropSubjectSearch27Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropSubjectSearch27Test extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
	{
		"resource": {
			"type": "record",
			"id": "109"
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
					"id": "carol"
				}
			]
		}
		""";
	}
}
