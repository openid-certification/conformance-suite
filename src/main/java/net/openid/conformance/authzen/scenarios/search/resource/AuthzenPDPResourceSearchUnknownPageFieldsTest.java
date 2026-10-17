package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-unknown-page-fields-ignored",
	displayName = "AuthZEN Resource Search API - Section 10.1.1: Unknown page fields ignored",
	summary = "Per Section 10.1.1, receivers MUST ignore unknown fields. `page` carries `customAttr`, a key no specification defines. Section 8.2.1 only allows additional page keys defined in a specification referenced in the Capabilities Registry, so this request breaks a rule for the sender on purpose: that rule does not tell the PDP to reject such a key, so the receiver rule of Section 10.1.1 applies. The PDP MUST return HTTP 200 with results that include record-1.\n" + AuthzenPDPResourceSearchUnknownPageFieldsTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchUnknownPageFieldsTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" },
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
					{ "type": "record", "id": "record-1" }
				]
			}
			""";
	}
}
