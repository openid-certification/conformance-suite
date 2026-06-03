package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-reject-empty-body",
	displayName = "Authzen Evaluation API - Section 10.1: Reject empty body",
	summary = "Section 10.1 — the PDP MUST return HTTP 400 when the request body is empty.",
	profile = "Authzen"
)
public class AuthzenPDPEvaluationRejectEmptyBodyTest extends AbstractAuthzenPDPEvaluationTest {

	// Placeholder JSON so the @PreEnvironment on the request body is satisfied; the
	// raw-body override sends an empty body to the PDP.
	public static final String payload = "{}";

	@Override
	protected String getPayload() {
		return payload;
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
