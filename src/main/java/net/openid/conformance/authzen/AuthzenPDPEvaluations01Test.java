package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-01",
	displayName = "Authzen Evaluations API Test 01",
	summary = "Evaluations API test 01 with payload\n" + AuthzenPDPEvaluations01Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPEvaluations01Test extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
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
			  "evaluations": [{ "decision": true }, { "decision": true }]
			 }
			""";
	}


	@Override
	protected JsonObject parseRequest() {
		return JsonParser.parseString(payload).getAsJsonObject();
	}

}
