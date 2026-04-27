package net.openid.conformance.authzen.interop.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-evaluation-10",
	displayName = "Authzen Evaluation API Test 10",
	summary = "Evaluation API test 10 with payload\n" + AuthzenPDPInteropEvaluation10Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropEvaluation10Test extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDE2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_read_user"
		},
		"resource": {
			"type": "user",
			"id": "morty@the-citadel.com"
		}
	}
	""";

	@Override
	protected String getPayload() {
		return payload;
	}
}
