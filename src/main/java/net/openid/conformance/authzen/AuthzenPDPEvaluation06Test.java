package net.openid.conformance.authzen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-evaluation-06",
	displayName = "Authzen Evaluation API Test 06",
	summary = "Evaluation API test 06 with payload\n" + AuthzenPDPEvaluation06Test.payload,
	profile = "Authzen",
	configurationFields = {
	}
)
public class AuthzenPDPEvaluation06Test extends AbstractAuthzenPDPTest {

	public static final String payload = """
	{
		"subject": {
			"type": "user",
			"id": "CiRmZDA2MTRkMy1jMzlhLTQ3ODEtYjdiZC04Yjk2ZjVhNTEwMGQSBWxvY2Fs"
		},
		"action": {
			"name": "can_update_todo"
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
	protected JsonObject parseRequest() {
		return JsonParser.parseString(payload).getAsJsonObject();
	}
}
