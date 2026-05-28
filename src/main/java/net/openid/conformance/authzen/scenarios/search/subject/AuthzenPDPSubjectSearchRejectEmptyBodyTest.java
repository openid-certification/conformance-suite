package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-reject-empty-body",
	displayName = "Authzen Subject Search API - Section 2.4.5: Reject empty body",
	summary = "Section 2.4.5 — the PDP MUST return HTTP 400 when the request body is empty.",
	profile = "Authzen"
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
	protected int getExpectedHttpStatusCode() {
		return 400;
	}
}
