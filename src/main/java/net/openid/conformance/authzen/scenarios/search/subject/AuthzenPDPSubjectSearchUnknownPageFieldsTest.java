package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-unknown-page-fields-ignored",
	displayName = "Authzen Subject Search API - Section 10.1.1: Unknown page fields ignored",
	summary = "Per spec 10.1.1-3, receivers MUST ignore unknown fields. `page` carries an unknown `customAttr`; PDP MUST return HTTP 200 with valid results.\n" + AuthzenPDPSubjectSearchUnknownPageFieldsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchUnknownPageFieldsTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"page": {
				"limit": 10,
				"customAttr": "ignored"
			}
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return """
			{
				"results": [
					{ "type": "user", "id": "alice" },
					{ "type": "user", "id": "bob" }
				]
			}
			""";
	}
}
