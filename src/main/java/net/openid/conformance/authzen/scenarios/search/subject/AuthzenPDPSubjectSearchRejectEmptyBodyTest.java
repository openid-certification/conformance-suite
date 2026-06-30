package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-reject-empty-body",
	displayName = "AuthZEN Subject Search API - Section 10.1: Reject empty body",
	summary = "Section 10.1 — the PDP MUST return HTTP 400 when the request body is empty.",
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchRejectEmptyBodyTest extends AbstractAuthzenPDPSubjectSearchTest {

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
