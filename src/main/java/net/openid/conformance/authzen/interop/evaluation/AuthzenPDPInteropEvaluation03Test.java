package net.openid.conformance.authzen.interop.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-evaluation-03",
	displayName = "Authzen Evaluation API Test 03",
	summary = "Evaluation API test 03 with payload\n" + AuthzenPDPInteropEvaluation03Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropEvaluation03Test extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_read_todos"
		},
		"resource": {
			"type": "todo",
			"id": "todo-1"
		}
	}
	""";

	@Override
	protected String getPayload() {
		return payload;
	}
}
