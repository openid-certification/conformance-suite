package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-unknown-top-level-fields-ignored",
	displayName = "AuthZEN Resource Search API - Section 10.1.1: Unknown top-level fields ignored",
	summary = "Per Section 10.1.1, receivers MUST ignore unknown fields in the request body. Adds `foo` and `futureField` at the top level alongside a valid resource search (records alice can read); PDP MUST return HTTP 200 with the results still including record-1.\n" + AuthzenPDPResourceSearchUnknownTopLevelFieldsTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchUnknownTopLevelFieldsTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" },
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
					{ "type": "record", "id": "record-1" }
				]
			}
			""";
	}
}
