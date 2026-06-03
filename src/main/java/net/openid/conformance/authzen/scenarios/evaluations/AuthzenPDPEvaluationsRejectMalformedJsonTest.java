package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-reject-malformed-json",
	displayName = "Authzen Evaluations API - Section 10.1: Reject malformed JSON",
	summary = "Section 10.1 — the PDP MUST return HTTP 400 when the request body is not valid JSON.",
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsRejectMalformedJsonTest extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = "{}";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedEvaluationsResponseJson() {
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
