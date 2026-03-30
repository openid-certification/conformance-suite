package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluations-02",
	displayName = "Authzen Evaluations API Test 02",
	summary = "Evaluations API test 02 with payload\n" + AuthzenPDPEvaluations02Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPEvaluations02Test extends AbstractAuthzenPDPEvaluationsTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDE2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
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
					"id": "7240d0db-8ff0-41ec-98b2-34a096273b91",
					"properties": {
						"ownerID": "morty@the-citadel.com"
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
			  "evaluations": [{ "decision": false }, { "decision": true }]
			 }
			""";
	}


	@Override
	protected JsonObject parseRequest() {
		return JsonParser.parseString(payload).getAsJsonObject();
	}

}
