package net.openid.conformance.authzen.interop.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-evaluation-34",
	displayName = "Authzen Evaluation API Test 34",
	summary = "Evaluation API test 34 with payload\n" + AuthzenPDPInteropEvaluation34Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropEvaluation34Test extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDQ2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_read_user"
		},
		"resource": {
			"type": "user",
			"id": "jerry@the-smiths.com"
		}
	}
	""";

	@Override
	protected String getPayload() {
		return payload;
	}
}
