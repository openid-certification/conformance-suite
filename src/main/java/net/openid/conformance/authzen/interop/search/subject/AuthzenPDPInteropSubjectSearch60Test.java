package net.openid.conformance.authzen.interop.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-subject-search-60",
	displayName = "Authzen Subject Search API Test 60",
	summary = "Subject Search API test 60 with payload\n" + AuthzenPDPInteropSubjectSearch60Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropSubjectSearch60Test extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
	{
		"resource": {
			"type": "record",
			"id": "120"
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
					"id": "bob"
				}
			]
		}
		""";
	}
}
