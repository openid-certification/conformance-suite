package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-reject-malformed-json",
	displayName = "Authzen Resource Search API - Section 2.4.4: Reject malformed JSON",
	summary = "Section 2.4.4 — the PDP MUST return HTTP 400 when the request body is not valid JSON.",
	profile = "Authzen"
)
public class AuthzenPDPResourceSearchRejectMalformedJsonTest extends AbstractAuthzenPDPResourceSearchTest {

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
