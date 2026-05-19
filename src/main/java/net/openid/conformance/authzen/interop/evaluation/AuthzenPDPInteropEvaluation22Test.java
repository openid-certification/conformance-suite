package net.openid.conformance.authzen.interop.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-evaluation-22",
	displayName = "Authzen Evaluation API Test 22",
	summary = "Evaluation API test 22 with payload\n" + AuthzenPDPInteropEvaluation22Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropEvaluation22Test extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDI2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_update_todo"
		},
		"resource": {
			"type": "todo",
			"id": "7240d0db-8ff0-41ec-98b2-34a096273b9f",
			"properties": {
				"ownerID": "summer@the-smiths.com"
			}
		}
	}
	""";

	@Override
	protected String getPayload() {
		return payload;
	}
}
