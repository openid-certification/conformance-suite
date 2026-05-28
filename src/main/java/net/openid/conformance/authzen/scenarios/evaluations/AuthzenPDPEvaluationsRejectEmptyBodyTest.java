package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-reject-empty-body",
	displayName = "Authzen Evaluations API - Section 2.4.5: Reject empty body",
	summary = "Section 2.4.5 — the PDP MUST return HTTP 400 when the request body is empty.",
	profile = "Authzen"
)
public class AuthzenPDPEvaluationsRejectEmptyBodyTest extends AbstractAuthzenPDPEvaluationsTest {

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
		return "";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}
}
