package net.openid.conformance.authzen.scenarios.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-resource-missing-id",
	displayName = "Authzen Evaluation API - Section 2.4.2: Resource missing id -- expect HTTP 400",
	summary = "Section 2.4.2 missing required sub-field. `resource` omits `id`; PDP MUST return HTTP 400.\n" + AuthzenPDPEvaluationResourceMissingIdTest.payload,
	profile = "Authzen"
)
public class AuthzenPDPEvaluationResourceMissingIdTest extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected int getExpectedHttpStatusCode() {
		return 400;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}
}
