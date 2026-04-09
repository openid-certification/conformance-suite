package net.openid.conformance.authzen.interop.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-evaluation-04",
	displayName = "Authzen Evaluation API Test 04",
	summary = "Evaluation API test 04 with payload\n" + AuthzenPDPInteropEvaluation04Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropEvaluation04Test extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_create_todo"
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
