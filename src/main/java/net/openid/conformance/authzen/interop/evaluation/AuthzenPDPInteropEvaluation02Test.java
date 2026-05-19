package net.openid.conformance.authzen.interop.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-evaluation-02",
	displayName = "Authzen Evaluation API Test 02",
	summary = "Evaluation API test 02 with payload\n" + AuthzenPDPInteropEvaluation02Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropEvaluation02Test extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_read_user"
		},
		"resource": {
			"type": "user",
			"id": "rick@the-citadel.com"
		}
	}
	""";

	@Override
	protected String getPayload() {
		return payload;
	}
}
