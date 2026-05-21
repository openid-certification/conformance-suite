package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-extra-resource-id-ignored",
	displayName = "Authzen Resource Search API - Spec 8.5.1-3: Extra resource.id ignored",
	summary = "Spec 8.5.1-3 says resource.id SHOULD be omitted from Resource Search and SHOULD be ignored if sent. This test sends a stray `resource.id` alongside the required `resource.type`; PDP MUST still return the expected results.\n" + AuthzenPDPResourceSearchExtraResourceIdIgnoredTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchExtraResourceIdIgnoredTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "ignored-stray-id" }
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
