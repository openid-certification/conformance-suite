package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-reject-malformed-json",
	displayName = "Authzen Evaluation API - Section 10.1: Reject malformed JSON",
	summary = "Section 10.1 — the PDP MUST return HTTP 400 when the request body is not valid JSON.",
	profile = "Authzen"
)
public class AuthzenPDPEvaluationRejectMalformedJsonTest extends AbstractAuthzenPDPEvaluationTest {

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
		// Deliberately broken JSON: missing closing brace and a stray identifier.
		return "{ \"subject\": { \"type\": \"user\", \"id\": \"alice\" } , broken";
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}
}
