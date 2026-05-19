package net.openid.conformance.authzen.interop.evaluations;

import net.openid.conformance.authzen.AbstractAuthzenPDPEvaluationsTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-interop-evaluations-03",
	displayName = "Authzen Evaluations API Test 03",
	summary = "Evaluations API test 03 with payload\n" + AuthzenPDPInteropEvaluations03Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPInteropEvaluations03Test extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDQ2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_update_todo"
		},
		"evaluations": [
			{
				"resource": {
					"type": "todo",
					"id": "7240d0db-8ff0-41ec-98b2-34a096273b92",
					"properties": {
						"ownerID": "rick@the-citadel.com"
					}
				}
			},
			{
				"resource": {
					"type": "todo",
					"id": "7240d0db-8ff0-41ec-98b2-34a096273b95",
					"properties": {
						"ownerID": "jerry@the-smiths.com"
					}
				}
			}
		]
	}
	""";

	@Override
	protected String getExpectedEvaluationsResponseJson() {
		return """
			 {
			  "evaluations": [{ "decision": false }, { "decision": false }]
			 }
			""";
	}


	@Override
	protected String getPayload() {
		return payload;
	}

}
