package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-unknown-top-level-fields-ignored",
	displayName = "AuthZEN Subject Search API - Section 10.1.1: Unknown top-level fields ignored",
	summary = "Per Section 10.1.1, receivers MUST ignore unknown fields in the request body. Adds `foo` and `futureField` at the top level alongside a valid subject search (users who can read record-1); PDP MUST return HTTP 200 with the results still including alice and bob.\n" + AuthzenPDPSubjectSearchUnknownTopLevelFieldsTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchUnknownTopLevelFieldsTest extends AbstractAuthzenPDPSubjectSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"foo": "bar",
			"futureField": { "nested": true }
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
