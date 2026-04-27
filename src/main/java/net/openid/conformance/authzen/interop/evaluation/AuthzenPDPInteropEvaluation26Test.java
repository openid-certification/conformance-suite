package net.openid.conformance.authzen.interop.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-evaluation-26",
	displayName = "Authzen Evaluation API Test 26",
	summary = "Evaluation API test 26 with payload\n" + AuthzenPDPInteropEvaluation26Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropEvaluation26Test extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDM2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_read_user"
		},
		"resource": {
			"type": "user",
			"id": "beth@the-smiths.com"
		}
	}
	""";

	@Override
	protected String getPayload() {
		return payload;
	}
}
