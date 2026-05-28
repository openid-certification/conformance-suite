package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-reject-malformed-json",
	displayName = "Authzen Action Search API - Section 2.4.4: Reject malformed JSON",
	summary = "Section 2.4.4 — the PDP MUST return HTTP 400 when the request body is not valid JSON.",
	profile = "Authzen"
)
public class AuthzenPDPActionSearchRejectMalformedJsonTest extends AbstractAuthzenPDPActionSearchTest {

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
		return "{ \"subject\": { \"type\": \"user\", \"id\": \"alice\" } , broken";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}
}
