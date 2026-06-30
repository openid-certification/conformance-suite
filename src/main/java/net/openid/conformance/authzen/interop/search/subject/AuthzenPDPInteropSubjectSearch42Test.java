package net.openid.conformance.authzen.interop.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-subject-search-42",
	displayName = "AuthZEN Subject Search API Test 42",
	summary = "Subject Search API test 42 with payload\n" + AuthzenPDPInteropSubjectSearch42Test.payload,
	profile = "AuthZEN",
	configurationFields = {
	}
)
public class AuthzenPDPInteropSubjectSearch42Test extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
	{
		"resource": {
			"type": "record",
			"id": "114"
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
