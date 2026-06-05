package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-extra-action-ignored",
	displayName = "Authzen Action Search API - Section 10.1.1: Extra action parameter ignored",
	summary = "Section 10.1.1 forward compatibility: receivers MUST ignore unknown fields in request bodies. Section 8.6.1 does not list `action` in the Action Search request schema, so an `action` field sent alongside the required `subject` and `resource` MUST be treated as an unknown field and ignored; the PDP MUST return the same list of actions it would return without it.\n" + AuthzenPDPActionSearchExtraActionIgnoredTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPActionSearchExtraActionIgnoredTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"resource": { "type": "record", "id": "record-1" },
			"action": { "name": "read" }
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
