package net.openid.conformance.authzen.scenarios.search.subject;

import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-reject-malformed-json",
	displayName = "Authzen Subject Search API - Section 2.4.4: Reject malformed JSON",
	summary = "Section 2.4.4 — the PDP MUST return HTTP 400 when the request body is not valid JSON.",
	profile = "Authzen"
)
public class AuthzenPDPSubjectSearchRejectMalformedJsonTest extends AbstractAuthzenPDPSubjectSearchTest {

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
		return "{ \"subject\": { \"type\": \"user\" } , broken";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}
}
