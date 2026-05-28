package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-reject-empty-body",
	displayName = "Authzen Action Search API - Section 2.4.5: Reject empty body",
	summary = "Section 2.4.5 — the PDP MUST return HTTP 400 when the request body is empty.",
	profile = "Authzen"
)
public class AuthzenPDPActionSearchRejectEmptyBodyTest extends AbstractAuthzenPDPActionSearchTest {

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
		return "";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}
}
