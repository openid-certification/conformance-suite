package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-unknown-page-fields-ignored",
	displayName = "AuthZEN Action Search API - Section 10.1.1: Unknown page fields ignored",
	summary = "Per Section 10.1.1, receivers MUST ignore unknown fields. `page` carries `customAttr`, a key no specification defines. Section 8.2.1 only allows additional page keys defined in a specification referenced in the Capabilities Registry, so this request breaks a rule for the sender on purpose: that rule does not tell the PDP to reject such a key, so the receiver rule of Section 10.1.1 applies. The PDP MUST return HTTP 200 with results that include read and write.\n" + AuthzenPDPActionSearchUnknownPageFieldsTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchUnknownPageFieldsTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
					{ "name": "read" },
					{ "name": "write" }
				]
			}
			""";
	}
}
