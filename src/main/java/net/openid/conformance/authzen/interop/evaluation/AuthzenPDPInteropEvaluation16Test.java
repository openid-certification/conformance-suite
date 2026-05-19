package net.openid.conformance.authzen.interop.evaluation;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-evaluation-16",
	displayName = "Authzen Evaluation API Test 16",
	summary = "Evaluation API test 16 with payload\n" + AuthzenPDPInteropEvaluation16Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropEvaluation16Test extends AbstractAuthzenPDPEvaluationTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDE2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_delete_todo"
		},
		"resource": {
			"type": "todo",
			"id": "7240d0db-8ff0-41ec-98b2-34a096273b9f",
			"properties": {
				"ownerID": "morty@the-citadel.com"
			}
		}
	}
	""";

	@Override
	protected String getPayload() {
		return payload;
	}
}
