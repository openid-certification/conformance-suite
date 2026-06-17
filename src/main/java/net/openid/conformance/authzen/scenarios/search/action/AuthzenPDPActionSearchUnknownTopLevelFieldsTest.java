package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-unknown-top-level-fields-ignored",
	displayName = "Authzen Action Search API - Section 10.1.1: Unknown top-level fields ignored",
	summary = "Per Section 10.1.1, receivers MUST ignore unknown fields in the request body. The action field is omitted entirely; adds `foo` and `futureField` at the top level alongside a valid action search (actions alice can perform on record-1); PDP MUST return HTTP 200 with the results still including read and write.\n" + AuthzenPDPActionSearchUnknownTopLevelFieldsTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPActionSearchUnknownTopLevelFieldsTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
					{ "name": "read" },
					{ "name": "write" }
				]
			}
			""";
	}
}
