package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-reject-empty-body",
	displayName = "AuthZEN Action Search API - Section 10.1: Reject empty body",
	summary = "Section 10.1 — the PDP MUST return HTTP 400 when the request body is empty.",
	profile = "AuthZEN"
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
	protected java.util.Set<Integer> getAcceptableHttpStatusCodes() {
		return java.util.Set.of(400);
	}
}
