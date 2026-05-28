package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-reject-top-level-array",
	displayName = "Authzen Action Search API - Spec 10.1.1-1: Reject top-level JSON array",
	summary = "Per spec 10.1.1-1, the top-level element of the request body MUST be a JSON object. The PDP MUST return HTTP 400 when sent a top-level array.",
	profile = "Authzen"
)
public class AuthzenPDPActionSearchRejectTopLevelArrayTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = "{}";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return "{}";
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected String getRawRequestBody() {
		return """
			[
				{
					"subject": { "type": "user", "id": "alice" },
					"resource": { "type": "record", "id": "record-1" }
				}
			]
			""";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}
}
