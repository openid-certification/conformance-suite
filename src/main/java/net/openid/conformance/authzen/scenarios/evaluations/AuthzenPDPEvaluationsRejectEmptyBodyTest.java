package net.openid.conformance.authzen.scenarios.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-reject-empty-body",
	displayName = "Authzen Evaluations API - Section 10.1: Reject empty body",
	summary = "Section 10.1 — the PDP MUST return HTTP 400 when the request body is empty.",
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
	protected java.util.Set<Integer> getAcceptableHttpStatusCodes() {
		return java.util.Set.of(400);
	}
}
